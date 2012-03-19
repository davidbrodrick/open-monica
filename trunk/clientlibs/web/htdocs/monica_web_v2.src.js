/*
 * open-monica Javascript client library v2
 * Copyright (C) 2011 Jamie Stevens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/**
 * @fileoverview This file contains the functions required to easily
 * interact with an open-monica (hereafter MoniCA) server. The code
 * here is adapted from the original version to follow some of the guidelines
 * in the O'Reilly book "Javascript - The Good Parts". This library
 * requires the Dojo toolkit (http://dojotoolkit.org).
 * @author ste616@gmail.com (Jamie Stevens)
 */

// Our Dojo requirements are listed here.
dojo.require('dojox.timing._base');

/**
 * This function returns an object through which the user may interact
 * easily with a MoniCA server. The user asks this object to connect or
 * disconnect, and can ask it to add or remove points to monitor.
 * @param {object} spec Specifications for the object setup.
 * @param {object} my   An object reference that this object should be able
 *                      to access.
 */
var monicaServer = function(spec, my) {
  /**
   * Some general purpose private variables.
   */
  // some counters for various functions
  var aPi, hPi, gDi, pPDi, rUi, aTULi, uPi, pPVi, iPi, gPi;
  // an array of point references to return to the add points caller.
  var pointReferences = [];

  // Variables in parsePointDescriptions.
  var descrRef;

  // Variables in getDescriptions.
  var requestString, added, gDhandle;

  // Variables in addToUpdateList.
  var pointFound;

  // Variables in updatePoints.
  var pollString, pollAdded, uPhandle, tR;

  // Variables in parsePointValues.
  var valRef;

  // Variables in addTimeSeries.
  var timeSeriesReference;

  // Variables in startTimeSeries.
  var seriesString, optionsObj, sTShandle;

  // Variables in comms.
  var postDeferred;

  // Variables in connect.
  var handle;
	
	// Variables in getPoints.
	var retArr;

  /**
   * The object that we will return to our caller.
   * @type {object}
   */
  var that = {};

  // Set some sensible defaults in the spec object.
  /**
   * The list of options controlling this object.
   * @type {object}
   */
  spec = spec || {};

  /**
   * The MoniCA server to connect to.
   * @type {string}
   */
  spec.serverName = spec.serverName || 'monhost-nar';

  /**
   * The protocol to use for connections.
   * @type {string}
   */
  spec.protocol = spec.protocol || 'http';

  /**
   * The web server hosting the MoniCA access scripts.
   * @type {string}
   */
  spec.webserverName = spec.webserverName || 'www.narrabri.atnf.csiro.au';

  /**
   * The full path on the webserver to the JSON MoniCA interface.
   * @type {string}
   */
  spec.webserverPath = spec.webserverPath ||
    'cgi-bin/obstools/web_monica/monicainterface_json.pl';

  /**
   * The time between queries to the MoniCA server for new values.
   * This should be a value in ms.
   * @type {int}
   */
  spec.updateInterval = spec.updateInterval || 10000;

  /**
   * A global error function to call if the connection fails.
   * @type {function}
   */
  spec.errorCallback = spec.errorCallback || handleErrors;
  
  /**
   * The names of all available data points on the MoniCA server.
   * @type {array}
   */
  var availablePointNames = [];

  /**
   * The data points we're responsible for.
   * @type {array}
   */
  var points = [];

  /**
   * A Dojo timing class that we use to trigger our updates.
   * @type {Timer}
   */
  var updateTimer = new dojox.timing.Timer();

  /**
   * A flag to indicate whether we have been asked to commence
   * periodic updating.
   * @type {boolean}
   */
  var isUpdating = false;

  /**
   * A list of points that have asked to be updated since our last
   * update call.
   * @type {array}
   */
  var requireUpdating = [];

	/**
	 * Our deferred promise for when we're loading.
	 * @type {Deferred}
	 */
	var loadingDeferred = new dojo.Deferred();
	
  // Our methods follow.

  // Our private methods.
  /**
   * Communicate with the specified MoniCA server.
   * @param {object} options All the required options for the request.
   */
  var comms = function(options) {
    // Check we have an options object.
    if (typeof options === 'undefined') {
      return undefined;
    }

    // Set our default error handler if not specified.
    options.errorCall = options.errorCall || spec.errorCallback;

    // Add the name of the server automatically.
    if (typeof options.content === 'undefined') {
      return undefined;
    }
    options.content.server = spec.serverName;

    /**
     * The return value from this function is a Dojo Deferred from the
     * xhrPost call.
     * @type {Deferred}
     */
    postDeferred = dojo.xhrPost({
      url: spec.protocol + '://' + spec.webserverName + '/' +
				spec.webserverPath,
      sync: false,
      content: options.content,
      handleAs: 'json',
      error: options.errorCall
    });

    return postDeferred;
  };

  /**
   * Deal with any error coming from an XHR post request.
   * @param {object} error The error coming back from an XHR post request.
   * @param {object} ioargs Something else.
   */
  var handleErrors = function(error, ioargs) {
    // This should really be something a bit nicer!
    alert(error);
  };

  /**
   * Parse a number of point names coming from a MoniCA server.
   * @param {object} data The data coming back from an XHR post request.
   * @param {object} ioargs Something else.
   */
  var parsePointNames = function(data, ioargs) {
    // The JSON coming back is already nicely formatted.
    availablePointNames = data.monitoringPointNames;

    return undefined; // no value comes from this chain
  };

  /**
   * Tell subscribers that we have connected to a MoniCA server.
   */
  var publishConnection = function() {
    // Indicate that we have connected by publishing the server name
    // to the 'connection' channel.
    dojo.publish('connection', [{ server: spec.serverName }]);
		// And by resolving our loading deferred object.
		loadingDeferred.resolve({
			server: spec.serverName
		});

    return undefined; // no value comes from this chain
  };

  /**
   * Check whether we have already made an object for a particular
   * MoniCA point.
   * @param {string} name The name of the MoniCA point to check for.
   */
  var hasPoint = function(name) {
    for (hPi = 0; hPi < points.length; hPi++) {
      if (name === points[hPi].getPointDetails().name) {
				return points[hPi];
      }
    }
    return undefined;
  };

  /**
   * Add a point to the list.
   * @param {object} pointRef A reference to the monicaPoint object
   */
  var addPoint = function(pointRef) {
    if (pointRef) {
      points.push(pointRef);
    }
  };

  /**
   * Parse some point descriptions coming from a MoniCA server, and give them
   * to the correct points.
   * @param {object} data The data coming back from an XHR post request.
   * @param {object} ioargs Something else.
   */
  var parsePointDescriptions = function(data, ioargs) {
    // An array of descriptions should come back in the 'data' item.
    if (data.data) {
      for (pPDi = 0; pPDi < data.data.length; pPDi++) {
				descrRef = hasPoint(data.data[pPDi].pointName);
				if (descrRef !== undefined) {
					descrRef.setPointDetails(data.data[pPDi]);
				}
      }
    }

    return undefined; // No value comes from this chain.
  };

  /**
   * Tell subscribers that we have gotten some descriptions.
   */
  var publishDescription = function() {
    // Indicate that we gotten some point descriptions by publishing
    // the server name to the 'description' channel.
    dojo.publish('description', [{ server: spec.serverName }]);

    return undefined; // No value comes from this chain.
  };

  /**
   * Add a point to the list of points requiring an update.
   * @param {object} uRef A monicaPoint reference.
   */
  var addToUpdateList = function(uRef) {
    // Check this point isn't already on the list.
    pointFound = false;
    for (aTULi = 0; aTULi < requireUpdating.length; aTULi++) {
      if (requireUpdating[aTULi] === uRef) {
				pointFound = true;
				break;
      }
    }
    if (pointFound === false) {
      requireUpdating.push(uRef);
    }
  };

  /**
   * Start a time-series.
   * @param {object} tRef A monicaPoint reference.
   */
  var startTimeSeries = function(tRef) {
    // Check we have all the required information and craft the request
    // object.
    seriesString = '';
    if (typeof tRef === 'undefined') {
      return;
    }

    seriesString += tRef.getPointDetails().name;
    // Get the current time-series options from the point.
    optionsObj = tRef.timeSeriesOptions();
    if (typeof optionsObj.startTime !== 'undefined') {
      seriesString += ',' + optionsObj.startTime;
    }
    if (typeof optionsObj.spanTime !== 'undefined') {
      seriesString += ',' + optionsObj.spanTime;
    }
    if (typeof optionsObj.maxPoints !== 'undefined') {
      seriesString += ',' + optionsObj.maxPoints;
    }

    /**
     * Get a handle to a Dojo Deferred that will retrieve the values for
     * the MoniCA point.
     * @type {Deferred}
     */
    sTShandle = comms({ content: { action: 'intervals',
				   points: seriesString } });

    // Check we get something back.
    if (!sTShandle) {
      alert('Internal calling error!');
      return;
    }

    // We will transfer the values to the appropriate point when the
    // result comes back.
    sTShandle = sTShandle.then(function(data, ioargs) {
      // The data should come back in the 'intervalData' item.
      if (typeof data.intervalData !== 'undefined') {
				// We only ever get one item back now.
				tRef.setTimeSeriesValues(data.intervalData[0]);
      }

      return undefined;
    });

    // Tell people new data is available.
    sTShandle = sTShandle.then(publishTimeSeries);
  };

  /**
   * Get values for all the points that require updating.
   */
  var updatePoints = function() {
    // Make a string to request the point values.
    pollString = '';
    pollAdded = 0;
    for (uPi = 0; uPi < requireUpdating.length; uPi++) {
      if (requireUpdating[uPi].isTimeSeries() === true &&
					requireUpdating[uPi].timeSeriesInitialised() === false) {
				// Get first values for time-series that
				// haven't yet been initialised.
				startTimeSeries(requireUpdating[uPi]);
				// We don't get any other points for him.
      } else {
				if (pollAdded > 0) {
					pollString += ';';
				}
				pollString += requireUpdating[uPi].getPointDetails().name;
				tR = requireUpdating[uPi].timeRepresentation();
				if (tR === 'unixms') {
					// We append the characters required to return the time
					// as Unix time in ms
					pollString += '...TD';
				} // We don't append anything for string time, since this is the
				// the default behaviour, and we can save on bandwidth.
				pollAdded++;
      }
    }

    // If we don't have any requests, don't do anything.
    if (pollString === '') {
      return;
    }

    // Empty the requireUpdating array.
    requireUpdating = [];

    /**
     * Get a handle to a Dojo Deferred that will retrieve the value for
     * the MoniCA points.
     * @type {Deferred}
     */
    uPhandle = comms({ content: { action: 'points', points: pollString } });

    // Check we get something back.
    if (!uPhandle) {
      // This shouldn't happen unless we don't pass any options to comms,
      // which should be impossible!
      alert('Internal calling error!');
      return;
    }

    // We will transfer the values to the appropriate points when
    // we are done.
    uPhandle = uPhandle.then(parsePointValues);
    // Tell people new data is available.
    uPhandle = uPhandle.then(publishValue);
  };

  /**
   * Parse some point values coming from a MoniCA server, and give them to
   * the correct points.
   * @param {object} data The data coming back from an XHR post request.
   * @param {object} ioargs Something else.
   */
  var parsePointValues = function(data, ioargs) {
    // An array of values should come back in the 'pointData' item.
    if (typeof data.pointData !== 'undefined') {
      for (pPVi = 0; pPVi < data.pointData.length; pPVi++) {
				if (data.pointData[pPVi].pointName !== '') {
					// Give the value to the point.
					valRef = hasPoint(data.pointData[pPVi].pointName);
					if (valRef !== undefined) {
						// Do a logical NOT on the error state to reverse the
						// MoniCA ASCII interface backwards error boolean.
						data.pointData[pPVi].errorState =
							!data.pointData[pPVi].errorState;
						valRef.updateValue(data.pointData[pPVi]);
					}
				}
      }
    }

    return undefined;
  };

  /**
   * Tell subscribers that we have gotten some new point values.
   */
  var publishValue = function() {
    // Indicate that we have gotten some point values by publishing
    // the server name to the 'value' channel.
    dojo.publish('value', [{ server: spec.serverName }]);

    return undefined;
  };

  /**
   * Tell subscribers that we have gotten some new time-series values.
   */
  var publishTimeSeries = function() {
    // Indicate that we have gotten some point values by publishing
    // the server name to the 'timeSeries' channel.
    dojo.publish('timeSeries', [{ server: spec.serverName }]);

    return undefined;
  };

  // Our public methods.
  /**
   * Connect to the MoniCA server and obtain a list of all the
   * monitoring points available from it.
   */
  that.connect = function() {
    /**
     * Get a handle to a Dojo Deferred that will retrieve the names of
     * the MoniCA points on the server.
     * @type {Deferred}
     */
    handle = comms({ content: { action: 'names' } });

    // Check we get something back.
    if (typeof handle === 'undefined') {
      // This shouldn't happen unless we don't pass any options to comms,
      // which should be impossible!
      alert('Internal calling error!');
      return null;
    }

    // Keep the list of point names once we get them.
    handle = handle.then(parsePointNames);
    // And then tell people we've connected.
    handle = handle.then(publishConnection);
		
		// Return our loading deferred object.
		return loadingDeferred;
  };

  /**
   * Return a list of the point names available from the MoniCA server.
   */
  that.pointsList = function() {
    return availablePointNames;
  };

	/**
	 * Indicate whether the named point exists on this server.
	 * @param {string} qPoint The point name to check for.
	 */
	that.isPoint = function(qPoint) {
		if (!dojo.isString(qPoint)) {
			return false;
		}
		
		for (iPi = 0; iPi < availablePointNames.length; iPi++) {
			if (availablePointNames[iPi] === qPoint) {
				return true;
			}
		}
		
		return false;
	};
	
  /**
   * Return the name of the server this object connects to.
   */
  that.getServerName = function() {
    return spec.serverName;
  };

  /**
   * Add some MoniCA points to keep updated.
   * @param {array} newPoints The name of the points to update.
   */
  that.addPoints = function(newPoints) {
    // Clear the references to the points we will return.
    pointReferences = [];
    for (aPi = 0; aPi < newPoints.length; aPi++) {
      // Check if we have this point already.
      pointReferences[aPi] = hasPoint(newPoints[aPi]);
      // And make a new point if we don't.
      if (pointReferences[aPi] === undefined) {
				pointReferences[aPi] = monicaPoint({
						pointName: newPoints[aPi]
				}, that);
				addPoint(pointReferences[aPi]);
				// We immediately add this to the list of points requiring
				// an update.
				addToUpdateList(pointReferences[aPi]);
      }
    }

    // Return references to all the objects that were requested.
    return pointReferences;
  };

	/**
	 * Get the point reference for a named point or set of points.
	 * @param {variable} pointNames A string or an array of strings,
	 *                              representing the points to return
	 *                              references for.
	 */
	that.getPoints = function(pointNames) {
		if (dojo.isArray(pointNames)) {
			// Get the reference for each point name.
			for (gPi = 0; gPi < pointNames.length; gPi++) {
				retArr[gPi] = hasPoint(pointNames[gPi]);
			}
			
			return retArr;
		} else if (dojo.isString(pointNames)) {
			return hasPoint(pointNames);
		} else {
			return null;
		}
	};
	
  /**
   * Add some MoniCA points to get time-series for.
   * @param {object} tsPoint The specification for the time-series.
   */
  that.addTimeSeries = function(tsPoint) {
    // Add the appropriate flags automatically.
    tsPoint.isTimeSeries = true;

    // We don't check for an existing time-series using the same
    // point, since we support having multiple time-series ranges
    // for the same point.
    timeSeriesReference = monicaPoint(tsPoint, that);

    // Add this point to our list.
    addPoint(timeSeriesReference);

    // We immediately add this to the list of points requiring
    // an update.
    addToUpdateList(timeSeriesReference);

    // Return the reference to this point.
    return timeSeriesReference;
  };

  /**
   * Get the descriptions from the MoniCA server for any points that
   * require this.
   * @param {object} pointReqst A reference to the object requesting
   *                            an individual update.
   */
  that.getDescriptions = function(pointReqst) {
    requestString = '';
    added = 0;
    if (typeof pointReqst !== 'undefined') {
      // We have a point requesting an individual update, so we just
      // get the description for it.
      requestString = pointReqst.getPointDetails().name;
    } else {
      // Work out which points we need to get descriptions for.
      for (gDi = 0; gDi < points.length; gDi++) {
				if (points[gDi].hasDescription() === false) {
					if (added > 0) {
						requestString += ';';
					}
					requestString += points[gDi].getPointDetails().name;
					added++;
				}
      }
    }

    // If we don't have any requests, don't do anything.
    if (requestString === '') {
      return;
    }

    /**
     * Get a handle to a Dojo Deferred that will retrieve the descriptions
     * of the MoniCA points.
     * @type {Deferred}
     */
    gDhandle = comms({ content: { action: 'descriptions',
				  points: requestString } });

    // Check we get something back.
    if (typeof gDhandle === 'undefined') {
      // This shouldn't happen unless we don't pass any options to comms,
      // which should be impossible!
      alert('Internal calling error!');
      return;
    }

    // We will transfer the descriptions to the appropriate points when
    // we are done.
    gDhandle = gDhandle.then(parsePointDescriptions);
    // Tell people we have some point descriptions.
    gDhandle = gDhandle.then(publishDescription);
  };

  /**
   * Request that some points be updated. This will primarily be used by the
   * points themselves, but may be called by any routine.
   * @param {array} uPoints An array of monicaPoint references, one for each
   *                       point that requires updating.
   */
  that.requestUpdate = function(uPoints) {
    for (rUi = 0; rUi < uPoints.length; rUi++) {
      addToUpdateList(uPoints[rUi]);
    }
  };

  /**
   * Demand that all currently requested points be updated immediately.
   */
  that.immediateUpdate = function() {
    updatePoints();
  };

  /**
   * Demand that this object begin automatic periodic updating.
   */
  that.startUpdating = function() {
    // Only works when we're not already updating.
    if (isUpdating === true) {
      return;
    }

    // Set the interval.
    updateTimer.setInterval(spec.updateInterval);

    // Call the update routine on each tick.
    updateTimer.onTick = updatePoints;

    // Mark us as updating.
    isUpdating = true;

    // Start the timer.
    updateTimer.start();
  };

  /**
   * Demand that this object cease automatic periodic updating.
   */
  that.stopUpdating = function() {
    updateTimer.stop();
    isUpdating = false;
  };
	
	/**
	 * Return our loading promise.
	 */
	that.getLoadingDeferred = function() {
		return loadingDeferred;
	};
	
	/**
	 * Set a point's value.
	 * @param {object} setDetails The object describing the setting.
	 */
	that.setPointValue = function(setDetails) {
		// The error checking is in the point methods, so we just check
		// for the sufficient number of properties here.
		if (typeof setDetails !== 'undefined' &&
				typeof setDetails.point !== 'undefined' &&
				typeof setDetails.value !== 'undefined' &&
				typeof setDetails.user !== 'undefined' &&
				typeof setDetails.pass !== 'undefined' &&
				typeof setDetails.type !== 'undefined') {
			var commsObj = {
				content: {
					action: 'setpoints',
					settings: setDetails.point + '$' + setDetails.value +
						'$' + setDetails.type + ';' + setDetails.user + '$' +
						setDetails.pass
				}
			};
			
			return comms(commsObj);
		} else {
			return null;
		}
	};

  // Return our object.
  return that;
};

/**
 * This function returns an object pertaining to a single MoniCA point.
 * It may however have many values for that point, as in the case of a time
 * series.
 * @param {object} spec Specifications for the object setup.
 * @param {object} my   An object reference that this object should be able
 *                      to access.
 */
var monicaPoint = function(spec, my) {
  // Some variables required in various functions.

  // Variables in addCallback.
  var i, callbackAdded;

  // Variables in updateValue.
  var j, nPointVal, nPointObj;

  // Variables in latestValue.
  var rIndex, rObj;

  // Variables in setTimeSeriesValues.
  var k;

  // Variables in getTimeSeries
  var tSArray, l;

  /**
   * The object that we will return to our caller.
   * @type {object}
   */
  var that = {};

  /**
   * The monicaServer object that we are attached to comes in as 'my'.
   * @type {object}
   */
  var serverObject = my;

  // Set some sensible defaults in the spec object.
  /**
   * This list of options controlling this object.
   * @type {object}
   */
  spec = spec || {};

  /**
   * The name of the point this object relates to.
   * @type {string}
   */
  spec.pointName = spec.pointName || '';

  /**
   * Is this point to be queried as a time-series?
   * @type {boolean}
   */
  spec.isTimeSeries = spec.isTimeSeries || false;

  /**
   * An object with the options for the time-series.
   * @type {object}
   */
  spec.timeSeriesOptions = spec.timeSeriesOptions ||
    {
      startTime: -1, // -1 means end at the current time.
      spanTime: 60, // In minutes.
      maxPoints: 500
    };

  /**
   * The description of this point.
   * @type {string}
   */
  var pointDescription = '';

  /**
   * The units this point is represented in.
   * @type {string}
   */
  var pointUnits = '';

  /**
   * The update time given by the server (seconds).
   * @type {number}
   */
  var pointUpdateTime = -1;

  /**
   * The values this point has.
   * @type {array}
   */
  var pointValues = [];

  /**
   * By default we allocate one monicaPointValue in this array.
   */
  pointValues[0] = monicaPointValue({}, that);

  /**
   * A status flag to indicate whether we have already got our time
   * series values.
   * @type {boolean}
   */
  var initTimeSeries = false;

  /**
   * A status flag to indicate whether our description has been obtained.
   * @type {boolean}
   */
  var descriptionAssigned = false;

  /**
   * A status flag to indicate whether our initial time-series range
   * has been obtained.
   * @type {boolean}
   */
  var timeSeriesObtained = false;

  /**
   * A Dojo timing class that we use to periodically add ourselves to
   * our server's update list.
   * @type {Timer}
   */
  var updateTimer = new dojox.timing.Timer();

  /**
   * A flag to indicate whether we are automatically requesting updates.
   * @type {boolean}
   */
  var isUpdating = false;

  /**
   * A list of callback routines to execute when we get updated.
   * @type {array}
   */
  var callbacks = [];

  /**
   * A flag to set whether we want our time assigned as Unix time in ms,
   * or as a string representation. By default we ask for it as a string.
   * @type {string}
   */
  var timeReprString = 'string';

  // Our private methods.
  /**
   * Add ourselves to our server's update list.
   */
  var requestUpdate = function() {
    serverObject.requestUpdate( [that] );
  };

  /**
   * Start our own automatic addition to the server's update list.
   */
  var configureTimer = function() {
    // Stop any timers that are currently being used.
    if (isUpdating === true) {
      updateTimer.stop();
      isUpdating = false;
    }

    // We use the value of pointUpdateTime if it is appropriate.
    if (pointUpdateTime !== -1) {
      updateTimer.setInterval(pointUpdateTime * 1000);
    }

    // Call the requestUpdate routine on each tick.
    updateTimer.onTick = requestUpdate;

    // Mark us as updating.
    isUpdating = true;

    // Start the timer.
    updateTimer.start();
  };

  /**
   * Stop our own automatic addition to the server's update list.
   */
  var stopTimer = function() {
    if (isUpdating === true) {
      updateTimer.stop();
      isUpdating = false;
    }
  };

  /**
   * If we are a time-series, calculate our update interval based on
   * the time span we're covering and the maximum number of points we're
   * able to have.
   */
  var calcUpdateInterval = function() {
    // Continue only if we're a time-series.
    if (spec.isTimeSeries === true) {
      pointUpdateTime = spec.timeSeriesOptions.spanTime * 60 /
				spec.timeSeriesOptions.maxPoints; // in seconds

      // Immediately change the timer if it is already running.
      if (isUpdating === true) {
				configureTimer();
      }
    }
  };
  // We call this now if we are being initialised as a time-series.
  if (spec.isTimeSeries === true) {
    calcUpdateInterval();
    // We also set our default time representation to Unix time.
    timeReprString = 'unixms';
  }

  // Our public methods.
  /**
   * Send back the details of this MoniCA point.
   */
  that.getPointDetails = function() {
    return ({
      'name': spec.pointName,
      'description': pointDescription,
      'units': pointUnits
    });
  };

  /**
   * Return whether we have had a description assigned to us.
   */
  that.hasDescription = function() {
    return descriptionAssigned;
  };

  /**
   * Get a description assigned to us.
   * @param {object} details The description, units and refresh time details.
   */
  that.setPointDetails = function(details) {
    // Check that the details object has come along.
    if (typeof details === 'undefined') {
      return;
    }

    // We may never override the user-defined setting of the settings here,
    // so that the user can customise how we are described and how often
    // we are updated.
    if (typeof details.description !== 'undefined' &&
				pointDescription === '') {
      pointDescription = details.description;
    }

    if (typeof details.units !== 'undefined' &&
				pointUnits === '') {
      pointUnits = details.units;
    }

    if (typeof details.updateTime !== 'undefined' &&
				pointUpdateTime === -1) {
      pointUpdateTime = details.updateTime;
    }

    descriptionAssigned = true;

    // We can now, by default, start ourselves updating.
    configureTimer();
  };

  /**
   * Reset the details that we have now, and ask for the server settings
   * again.
   */
  that.resetPointDetails = function() {
    pointDescription = '';
    pointUnits = '';
    pointUpdateTime = -1;

    descriptionAssigned = false;

    serverObject.getDescriptions(that);
  };

  /**
   * Update our value. The MoniCA server should be doing this most of the
   * time, but we make it a public method so anyone can do it. This will come
   * in handy when the point is settable.
   * @param {object} newValues The set of parameters describing the
   *                           point's value and state.
   */
  that.updateValue = function(newValues) {
    // Check that the newValues object has come along.
    if (typeof newValues === 'undefined') {
      return;
    }

    if (typeof newValues.value !== 'undefined' &&
				typeof newValues.time !== 'undefined') {
      if (spec.isTimeSeries === false) {
				// Just replace the only value.
				pointValues[0].setValue(newValues);
      } else {
				// Add this point the end of the array, if the time is different to
				// the current last point.
				if (newValues.time !==
						pointValues[pointValues.length - 1].getValue().time) {
					pointValues.push(monicaPointValue({ initialValue: newValues },
					that));

					// Check we haven't got more than the maximum number of points
					// we're allowed to have, and remove some if we do.
					while (spec.timeSeriesOptions.maxPoints > 0 &&
								 pointValues.length > spec.timeSeriesOptions.maxPoints) {
						pointValues.shift();
					}
				}
      }
    }

    // Execute any callbacks we have.
    for (j = 0; j < callbacks.length; j++) {
      callbacks[j](that);
    }
  };

  /**
   * Take a series of values from a time-series call and fill our
   * time-series variables appropriately.
   * @param {object} tValues The values for the time-series.
   */
  that.setTimeSeriesValues = function(tValues) {
    // Check that we are actually a time-series!
    if (spec.isTimeSeries === false) {
      return;
    }
    // Check that we have some data coming back.
    if (typeof tValues.data !== 'undefined') {
      for (k = 0; k < tValues.data.length; k++) {
				if (k >= pointValues.length) {
					pointValues.push(monicaPointValue({ initialValue: tValues.data[k] },
					that));
				} else {
					pointValues[k].setValue(tValues.data[k]);
				}
      }
    }

    // We don't need to get this filled anymore.
    initTimeSeries = true;

    // Execute any callbacks we have.
    for (k = 0; k < callbacks.length; k++) {
      callbacks[k](that);
    }
  };

  /**
   * Return the whole series of values from our time-series set
   * to the caller.
   * @param {object} gOptions The options to be passed to the point value
   *                          specifying the format to return the points in.
   */
  that.getTimeSeries = function(gOptions) {
    // Check that we are actually a time-series!
    if (spec.isTimeSeries === false) {
      return undefined;
    }

    /**
     * Unless specified otherwise, we will default to returning
     * an array useful for the plotting library Highcharts.
     */
    if (typeof gOptions === 'undefined') {
      gOptions = {};
    }

    if (typeof gOptions.arrayHighcharts === 'undefined') {
      gOptions.arrayHighcharts = true;
    }

    /**
     * The array of values returned for the time-series.
     * @type {array}
     */
    tSArray = [];

    for (l = 0; l < pointValues.length; l++) {
      tSArray.push(pointValues[l].getValue(gOptions));
    }

    // Now just return the whole array of our point values.
    return tSArray;
  };

  /**
   * Retrieve the latest value from this point.
   * @param {object} gOptions The options to be passed to the point value
   *                          specifying the format to return the points in.
   */
  that.latestValue = function(gOptions) {
    // What we do depends on whether we are a time-series or not.
    if (spec.isTimeSeries === false) {
      // We simply return the only value.
      rIndex = 0;
    } else {
      // We return the last value
      rIndex = pointValues.length - 1;
    }

    // If we are a time-series, then we set by default to get the
    // latest value in a format suitable for Highcharts.
    if (typeof gOptions === 'undefined') {
      gOptions = {};
    }

    if (spec.isTimeSeries === true &&
				typeof gOptions.arrayHighcharts === 'undefined') {
      gOptions.arrayHighcharts = true;
    }

    return pointValues[rIndex].getValue(gOptions);
  };

  /**
   * Add a callback routine to our list.
   * @param {function} fn The reference to a callback function we are to
   *                      execute each time we are updated.
   */
  that.addCallback = function(fn) {
    callbackAdded = false;
    for (i = 0; i < callbacks.length; i++) {
      if (fn === callbacks[i]) {
				callbackAdded = true;
				break;
      }
    }

    if (callbackAdded === false) {
      callbacks.push(fn);
    }
  };

  /**
   * Change the time-series options.
   * @param {object} tsOptions The options for the time-series.
   */
  that.timeSeriesOptions = function(tsOptions) {
    // Check that we get an options object.
    if (typeof tsOptions === 'undefined') {
      // If there are no options given, we pass the current options
      // back to the caller.
      return spec.timeSeriesOptions;
    }

    if (typeof tsOptions.startTime !== 'undefined') {
      spec.timeSeriesOptions.startTime = tsOptions.startTime;
    }

    if (typeof tsOptions.spanTime !== 'undefined') {
      spec.timeSeriesOptions.spanTime = tsOptions.spanTime;
    }

    if (typeof tsOptions.maxPoints !== 'undefined') {
      spec.timeSeriesOptions.maxPoints = tsOptions.maxPoints;
    }

    // Recalculate the update interval with the new values.
    calcUpdateInterval();

    // We return the altered options object.
    return spec.timeSeriesOptions;
  };

  /**
   * Return whether we are a time-series or not.
   */
  that.isTimeSeries = function() {
    return spec.isTimeSeries;
  };

  /**
   * Return whether we have gotten our first time-series values.
   */
  that.timeSeriesInitialised = function() {
    return initTimeSeries;
  };

  /**
   * Return the time representation indicator.
   */
  that.timeRepresentation = function() {
    return timeReprString;
  };

  /**
   * Stop our automatic update procedures.
   */
  that.stopUpdating = function() {
    stopTimer();
  };

  /**
   * Start our automatic update procedures.
   */
  that.startUpdating = function() {
    configureTimer();
  };
	
	/**
	 * Ask the server to set this point to a new value.
	 * @param {object} setDetails The object describing the setting.
	 */
	that.setValue = function(setDetails) {
		// Check we have a sufficient set of details.
		if (typeof setDetails === 'undefined') {
			return null;
		}
		
		if (typeof setDetails.value === 'undefined' ||
				typeof setDetails.user === 'undefined' ||
				typeof setDetails.pass === 'undefined') {
			return null;
		}
		
		// Have we got a specified type?
		if (typeof setDetails.type === 'undefined') {
			// Try to figure it out.
			if (dojo.isString(setDetails.value) === true) {
				setDetails.type = 'str';
			} else if (setDetails.value === true ||
								 setDetails.value === false) {
				setDetails.type = 'bool';
			} else {
				// Too generic.
				return null;
			}
		}
		
		// Call the server now.
		setDetails.point = spec.pointName;
		var setPromise = new dojo.Deferred;
		serverObject.setPointValue(setDetails).then(function(data, ioargs) {
			if (typeof data !== 'undefined' &&
					typeof data.setResult !== 'undefined') {
				setPromise.resolve(data.setResult[0]);
			} else {
				setPromise.reject('Error while setting point value.');
			}
		}, function(error, ioargs) {
			setPromise.reject('Error while setting point value.');
		});
		
		return setPromise;
	};

  // return our object
  return that;
};

/**
 * This function returns an object pertaining to a single MoniCA point
 * value. We code the value in this way so that the value can "take care
 * of itself" - so to speak.
 * @param {object} spec Specifications for the object setup.
 * @param {object} my An object reference that this object should be able
 *                    to access.
 */
var monicaPointValue = function(spec, my) {
  // Some variables required in our functions.

  // Variables in getValue.
  var alteredValue;

  // Variables in copyValue.
  var nObject;

  /**
   * The object that we will return to our caller.
   * @type {object}
   */
  var that = {};

  /**
   * The monicaPoint object that we are attached to comes in as 'my'.
   * @type {object}
   */
  var pointObject = my;

  // Set some sensible defaults in the spec object.
  if (typeof spec === 'undefined') {
    spec = {};
  }

  // We store our point internally as an object with a few parameters.
  var ourValue = {
    value: '',
    time: '',
    errorState: true
  };

  // Our private methods.
  /**
   * Make a copy of the value object and return it.
   */
  var copyValue = function() {
    // Make a blank object as the destination.
    nObject = {};

    nObject.value = ourValue.value;
    nObject.time = ourValue.time;
    nObject.errorState = ourValue.errorState;

    return nObject;
  };
  /**
   * Removes a silly symbol that may come back with values that are in
   * degrees.
   * @param {string} bValue The value requiring treatment.
   */
  var stripBadChars = function(bValue) {
    if (isString(bValue)) {
      return bValue.replace(/[\u00c2><]/g, '');
    }
    return bValue;
  };

  /**
   * Checks to see if the passed parameter is a number.
   * @param {something} cValue The parameter which should be checked for
   *                           numberonomy.
   */
  var isNumber = function(cValue) {
    return typeof cValue === 'number' && isFinite(cValue);
  };

  /**
   * Checks to see if the passed parameter is a string.
   * @param {something} cValue The parameter which should be checked for
   *                            stringiness.
   */
  var isString = function(cValue) {
    return typeof cValue === 'string';
  };

  // Our public methods.
  /**
   * Update our value.
   * @param {object} newValue The set of parameters describing the point's
   *                          value and state. This may however be an array
   *                          if the value is coming from a time series.
   */
  that.setValue = function(newValue) {
    // Check we have something passed to us.
    if (typeof newValue !== 'undefined') {
      if (newValue instanceof Array) {
				// We have an array passed to us, which we now deal with.
				if (newValue.length < 2) {
					// We need two elements minimum to continue.
					return;
				}
				ourValue.value = stripBadChars(newValue[1]);
				ourValue.time = newValue[0];
				if (newValue.length === 3) {
					// We also have an error condition.
					ourValue.errorState = newValue[2];
				}
      } else {
				if (typeof newValue.value !== 'undefined') {
					// Always treat our value for the bad characters.
					ourValue.value = stripBadChars(newValue.value);
				}

				if (typeof newValue.time !== 'undefined') {
					ourValue.time = newValue.time;
				}

				if (typeof newValue.errorState !== 'undefined') {
					ourValue.errorState = newValue.errorState;
				}
      }
    }
  };

  // Immediately set our value if we were initialised with a value.
  if (typeof spec.initialValue !== 'undefined') {
    that.setValue(spec.initialValue);
  }

  /**
   * Return our value.
   * @param {object} getOptions A set of parameters specifying how the
   *                            caller wants the value to be formatted.
   */
  that.getValue = function(getOptions) {
    if (typeof getOptions === 'undefined') {
      // With no options, we simply return our value as is.
      return ourValue;
    }

    // From now on we'll deal with a copy of our value object so it
    // can be altered in ways the caller specifies.
    alteredValue = copyValue();

    // We do any alterations to our value first.
    if (typeof getOptions.valueAsDecimalDegrees !== 'undefined' &&
				getOptions.valueAsDecimalDegrees === true) {
      // Change the value into decimal degrees (only appropriate
      // if the value is an angle)
      if (isString(alteredValue.value)) {
				alteredValue.value = alteredValue.value.sexagesimalToDecimal();
      }
    }

    // We deal with any requests for how the value should be returned
    // from here.
    if (typeof getOptions.arrayHighcharts !== 'undefined' &&
				getOptions.arrayHighcharts === true) {
      // Return an array with the time as element 0, value as 1
      return ([alteredValue.time, alteredValue.value]);
    }

    // If we get here, we've failed, so we just return our value.
    return alteredValue;
  };

  // Return our object to the caller.
  return that;
};

/**
 * Extend the String object with a method that converts a sexagesimal
 * formatted string into a decimal number.
 */
String.prototype.sexagesimalToDecimal = function() {
  // Variables in this method
  var matchEls, dd, mm, ss, sign, formMatch, decRep;

  /**
   * An indicator that goes true when the string looks like a
   * sexagesimal quantity.
   * @type {boolean}
   */
  formMatch = false;

  // Check that the string looks like a known sexagesimal format.
  if (/^[\+\-]*\d+\?\d+\'\d+\"\.\d*$/.test(this)) {
    matchEls = this.match(/^([\+\-]*)(\d+)\?(\d+)\'(\d+)\"\.(\d*)$/);
    sign = matchEls[1] !== '' ? matchEls[1] : '+';
    dd = parseInt(matchEls[2]);
    mm = parseInt(matchEls[3]);
    ss = parseInt(matchEls[4]) +
    parseInt(matchEls[5]) / Math.pow(10, matchEls[5].length);
    formMatch = true;
  } else if (/^[\+\-]*\d+\:\d+\:\d+$/.test(this)) {
    matchEls = this.match(/^([\+\-]*)(\d+)\:(\d+)\:(\d+)$/);
    sign = matchEls[0] !== '' ? matchEls[0] : '+';
    dd = parseInt(matchEls[1]);
    mm = parseInt(matchEls[2]);
    ss = parseInt(matchEls[3]);
    formMatch = true;
  }

  if (formMatch === true) {
    decRep = dd + mm / 60 + ss / 3600;
    if (sign === '-') {
      decRep *= -1;
    }

    return decRep;
  } else {
    // The string wasn't recognisably sexagesimal, so we just return
    // it unchanged.
    return this;
  }
};
