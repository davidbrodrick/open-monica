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
  var aPi, hPi, gDi, pPDi, rUi, aTULi, uPi, pPVi, iPi, gPi, pPVj, pPDj, aPj;
  var hAi, gAli;
  // an array of point references to return to the add points caller.
  var pointReferences = [];
  
  // Variables in addPoints.
  var canAdd, tempAdd;

  // Variables in parsePointDescriptions.
  var descrRef;

  // Variables in getDescriptions.
  var requestString, added, gDhandle;

  // Variables in addToUpdateList.
  var pointFound;

  // Variables in updatePoints.
  var pollString, pollAdded, uPhandle, tR, uPalarm;

  // Variables in parsePointValues.
  var valRef;

  // Variables in addTimeSeries.
  var timeSeriesReference;

  // Variables in startTimeSeries.
  var seriesString, optionsObj, sTShandle;

  // Variables in connect.
  var handle;
	
	// Variables in getPoints.
	var retArr;
  
  // Variables in removePoint(s).
  var rPi, rPref;

  // Variables in getAlarms.
  var gAhandle, gAi, gAj, gAget, gAneedDesc, gAnewAlarm;
  var gAretnArr;

  // Variables in findAlarm.
  var fAi;

  // Variables in addAlarmCallback.
  var aACadded, aACi;

  // Variables in updateAlarmAuthData.
  var uAADi, uAADa;

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
   * An optional feature to make this handler call getDescriptions
   * automatically at the beginning of every update. This can be
   * useful if you are handling point addition on a point-by-point
   * basis but want to take advantage of the bundling ability of the
   * handler.
   * @type {boolean}
   */
  spec.autoDescriptions = spec.autoDescriptions || false;
  
  /**
   * The time between calls to the server to determine which states
   * are producing alarms. Not all MoniCA servers may be configured in
   * this way, so a value of zero here disables this periodic polling.
   * @type {number}
   */
  spec.alarmPollPeriod = spec.alarmPollPeriod || 0;

  /**
   * A list of functions to call each time the alarms are polled.
   * @type {array}
   */
  spec.alarmCallbacks = spec.alarmCallbacks || [];

  /**
   * The authentication information to use when shelving or
   * acknowledging alarms.
   * @type {object}
   */
  spec.alarmAuthData = spec.alarmAuthData || {};
  
  /**
   * The username to authenticate with.
   * @type {string}
   */
  spec.alarmAuthData.user = spec.alarmAuthData.user || '';

  /**
   * The password to authenticate with.
   * @type {string}
   */
  spec.alarmAuthData.pass = spec.alarmAuthData.pass || '';
      
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
   * The alarm objects we know about.
   * @type {array}
   */
  var alarms = [];

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
   * A Dojo timing class that we use to trigger our alarm polling.
   * @type {Timer}
   */
  var alarmPollTimer = new dTime.Timer();

  /**
   * A flag to indicate whether we have been asked to commence
   * periodic polling of the alarms.
   * @type {boolean}
   */
  var isPolling = false;

  /**
   * A list of points that have asked to be updated since our last
   * update call.
   * @type {array}
   */
  var requireUpdating = [];

  /**
   * The list of point references who are just about to receive data,
   * in the order this module has requested them.
   * @type {array}
   */
  var updatesArriving = [];
  
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
    var postDeferred = dojo.xhrPost({
      url: spec.protocol + '://' + spec.webserverName + '/' +
				spec.webserverPath,
      sync: false,
      content: options.content,
      handleAs: 'json',
      error: options.errorCall,
      failOK: true
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
   * @param {variable} name The name of the MoniCA point to check for,
   *                        or a point reference.
   */
  var hasPoint = function(name) {
    var retArr = [];
    for (hPi = 0; hPi < points.length; hPi++) {
      if (dojo.isString(name) === true) {
        if (name === points[hPi].getPointDetails().name) {
          retArr.push(points[hPi]);
        }
      } else {
        if (name === points[hPi]) {
          retArr.push({
            pointRef: points[hPi],
            index: hPi
          });
        }
      }
    }

    if (retArr.length > 0) {
      return retArr;
    }

    return undefined;
  };

  /**
   * Check whether we have already made an object for a particular
   * MoniCA alarm.
   * @param {variable} name The name of the MoniCA alarm to check for,
   *                        or an alarm reference.
   */
  var hasAlarm = function(name) {
	  var retArr = [];
	  for (hAi = 0; hAi < alarms.length; hAi++) {
	    if (dojo.isString(name) === true) {
		    if (name === alarms[hAi].getState().pointName) {
		      retArr.push(alarms[hAi]);
		    }
	    } else {
		    if (name === alarms[hAi]) {
		      retArr.push({
			      pointRef: alarms[hAi],
				    index: hAi
				  });
		    }
	    }
	  }
	  
	  if (retArr.length > 0) {
	    return retArr;
	  }
	  
    return undefined;
  };

  /**
   * Add a point to the list.
   * @param {object} pointRef A reference to the monicaPoint object.
   */
  var addPoint = function(pointRef) {
    if (typeof pointRef !== 'undefined') {
      points.push(pointRef);
    }
  };

  /**
   * Remove a point from the list.
   * @param {object} pointRef A reference to the monicaPoint object.
   */
  var removePoint = function(pointRef) {
    if (typeof pointRef !== 'undefined') {
      rPref = hasPoint(pointRef);
      // hasPoint returns an array, but should really be able to return
      // only a single value when dealing with a reference.
      if (rPref[0].pointRef === pointRef) {
        // Do some cleanup before we get rid of it.
        pointRef.stopTimer();
        points.splice(rPref[0].index, 1);
      }
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
          for (pPDj = 0; pPDj < descrRef.length; pPDj++) {
				    descrRef[pPDj].setPointDetails(data.data[pPDi]);
          }
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
   * Find and return a named alarm point.
   * @param {String} alarmName The name of the alarm to search for.
   */
  var findAlarm = function(alarmName) {
	  for (fAi = 0; fAi < alarms.length; fAi++) {
	    if (alarms[fAi].getState().pointName === alarmName) {
		    return alarms[fAi];
	    }
	  }
	  return null;
  };

  /**
   * Get the server's alarm state.
   */
  var getAlarms = function() {
	  /**
	   * Get a handle to a Dojo Deferred that will retrieve the values for
	   * the MoniCA alarms.
	   * @type {Deferred}
	   */
	  gAhandle = comms({ content: { action: 'alarms' } });

    // Check we get something back.
	  if (!gAhandle) {
      alert('Internal calling error!');
	    return;
	  }

    // We need to reset all the alarm states before we get the new states,
    // because only alarms that are "alarmed" will be returned by this call.
    for (gAi = 0; gAi < alarms.length; gAi++) {
      alarms[gAi].alarmOff();
    }
        
    // We will transfer the values to the appropriate alarm point
	  // when the result comes back.
    gAhandle = gAhandle.then(function(data, ioargs) {
		  if (typeof data.alarmStates !== 'undefined') {
		    gAneedDesc = false;
		    for (gAi = 0; gAi < data.alarmStates.length; gAi++) {
			    // Check for an already existing alarm point.
			    gAget = findAlarm(data.alarmStates[gAi].pointName);
			    if (gAget === null) {
			      gAneedDesc = true;
            gAnewAlarm = monicaAlarm(data.alarmStates[gAi], that);
			      alarms.push(gAnewAlarm);
            // Set up the point with any global information.
            gAnewAlarm.setAuthData(spec.alarmAuthData);
            for (gAj = 0; gAj < spec.alarmCallbacks.length; gAj++) {
              gAnewAlarm.addCallback(spec.alarmCallbacks[gAj]);
            }
            gAnewAlarm.fireCallbacks();
			    } else {
			      gAget.updateState(data.alarmStates[gAi]);
			    }
		    }
		    if (gAneedDesc) {
			    that.getDescriptions();
		    }
		  }
		  
      for (gAi = 0; gAi < alarms.length; gAi++) {
        alarms[gAi].fireCallbacks();
      }
	  });

  };

  /**
   * Convert a length 1 array into a single value.
   * @param {array} tArr The array that may have a length of one.
   */
  var reduceArray = function(tArr) {
    if (dojo.isArray(tArr) !== 'true') {
      return tArr;
    }
    if (tArr.length === 1) {
      return tArr[0];
    } else {
      return tArr;
    }
  };
  
  /**
   * Get values for all the points that require updating.
   */
  var updatePoints = function() {
    // Get the descriptions of any new points if required.
    if (spec.autoDescriptions === true) {
      that.getDescriptions();
    }
    // Make a string to request the point values.
    pollString = '';
    pollAdded = 0;
    updatesArriving = [];
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
        updatesArriving.push(requireUpdating[uPi]);
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
            for (pPVj = 0; pPVj < valRef.length; pPVj++) {
              // Check for an array of possible updaters.
              if (valRef.length > 1) {
                // Check that the right point gets the update.
                if (valRef[pPVj] !== updatesArriving[pPVi]) {
                  continue;
                }
              }
					    valRef[pPVj].updateValue(data.pointData[pPVi]);
            }
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
      tempAdd = hasPoint(newPoints[aPi]);
      // And make a new point if we don't.
      canAdd = true;
      if (tempAdd !== undefined) {
        // There is already a point with this name, but it may
        // be a time series.
        for (aPj = 0; aPj < tempAdd.length; aPj++) {
          if (tempAdd[aPj].isTimeSeries() === false) {
            // This is a point reference, so we don't need to add
            // this point.
            canAdd = false;
            pointReferences[aPi] = tempAdd[aPj];
          }
        }
      }
      if (canAdd === true) {
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
   * Remove some MoniCA points from our update list.
   * @param {array} oldPoints The references for the points to remove.
   */
  that.removePoints = function(oldPoints) {
    if (dojo.isArray(oldPoints) === false) {
      return;
    }
    for (rPi = 0; rPi < oldPoints.length; rPi++) {
      removePoint(oldPoints[rPi]);
    }
  };
  
	/**
	 * Get the point reference for a named point or set of points.
	 * @param {variable} pointNames A string or an array of strings,
	 *                              representing the points to return
	 *                              references for.
	 */
	that.getPoints = function(pointNames) {
		if (dojo.isArray(pointNames)) {
			// Get the references for each point name.
			for (gPi = 0; gPi < pointNames.length; gPi++) {
				retArr[gPi] = reduceArray(hasPoint(pointNames[gPi]));
			}
			
			return retArr;
		} else if (dojo.isString(pointNames)) {
			return reduceArray(hasPoint(pointNames));
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

  /**
   * Acknowledge or unacknowledge an alarm.
   * @param {object} ackDetails The object describing the acknowledgement.
   */
  that.acknowledgeAlarm = function(ackDetails) {
    // Check for the required properties here.
    if (typeof ackDetails !== 'undefined' &&
        typeof ackDetails.point !== 'undefined' &&
        typeof ackDetails.value !== 'undefined' &&
        typeof ackDetails.user !== 'undefined' &&
        typeof ackDetails.pass !== 'undefined') {
      var commsObj = {
        content: {
          action: 'alarmack',
          acknowledgements: ackDetails.point + '$' +
            ackDetails.value + ';' + ackDetails.user + '$' +
            ackDetails.pass
        }
      };
          
      return comms(commsObj);
    } else {
      return null;
    }
  };

  /**
   * Shelve or unshelve
   * @param {object} shelveDetails The object describing the shelving.
   */
  that.shelveAlarm = function(shelveDetails) {
    // Check for the required properties here.
    if (typeof shelveDetails !== 'undefined' &&
        typeof shelveDetails.point !== 'undefined' &&
        typeof shelveDetails.value !== 'undefined' &&
        typeof shelveDetails.user !== 'undefined' &&
        typeof shelveDetails.pass !== 'undefined') {
      var commsObj = {
        content: {
          action: 'alarmshelve',
          shelves: shelveDetails.point + '$' +
            shelveDetails.value + ';' + shelveDetails.user + '$' +
            shelveDetails.pass
        }
      };
          
      return comms(commsObj);
    } else {
      return null;
    }
  };
      
  /**
   * Start polling of the server's alarms.
   * @param {number} period An optional new period to use for the
   *                        alarm polling.
   */
  that.startAlarmPolling = function(period) {
    if (typeof period !== 'undefined') {
      spec.alarmPollPeriod = period;
    }
    if (spec.alarmPollPeriod > 0) {
      // Set the interval.
      alarmPollTimer.setInterval(spec.alarmPollPeriod);
          
      // Call the update routine on each tick.
      alarmPollTimer.onTick = getAlarms;
          
      // Mark us as updating.
      isPolling = true;
          
      // Start the timer.
      alarmPollTimer.start();
    }
  };

  /**
   * Stop polling of the server's alarms.
   */
  that.stopAlarmPolling = function() {
    alarmPollTimer.stop();
    isPolling = false;
  };
      
  /**
   * Demand that the server get the alarm states immediately.
   */
  that.immediateAlarmPoll = function() {
    getAlarms();
  };

  /**
   * Get the alarm reference for a named alarm or a set of alarms.
   * @param {variable} alarmNames A string or an array of strings,
   *                              representing the alarms to return
   *                              references for.
   */
  that.getAlarms = function(alarmNames) {
    gAretnArr = [];
    if (dojo.isArray(alarmNames)) {
      for (gAli = 0; gAli < alarmsNames.length; gAli++) {
        gAretnArr[gAli] = reduceArray(hasAlarm(alarmNames[gAli]));
      }
      return gAretnArr;
    } else if (dojo.isString(alarmNames)) {
      return reduceArray(hasAlarm(alarmNames));
    } else {
      return null;
    }
  };
      
  /**
   * Add a callback to the global list of alarm point callbacks.
   * @param {function} nFunc The function to add to the callback list.
   * @param {boolean} updAll An optional flag to specify that all the
   *                         current alarm points should be given this
   *                         callback immediately.
   */
  that.addAlarmCallback = function(nFunc, updAll) {
    aACadded = false;
    for (aACi = 0; aACi < spec.alarmCallbacks.length; aACi++) {
      if (nFunc === spec.alarmCallbacks[aACi]) {
        aACadded = true;
        break;
      }
    }
        
    if (aACadded === false) {
      spec.alarmCallbacks.push(nFunc);
      if (updAll) {
        for (aACi = 0; aACi < alarms.length; aACi++) {
          alarms[aACi].addCallback(nFunc);
        }
      }
    }
  };

  /**
   * Update the alarm acknowledge/shelve authentication data.
   * @param {object} authData The username and password object that
   *                          should be used for authentication with
   *                          the server's alarm system.
   * @param {boolean} updAll An optional flag to specify that all the
   *                         current alarm points should be updated
   *                         immediately.
   */
  that.updateAlarmAuthData = function(authData, updAll) {
    uAADa = false;
    if (typeof authData.user !== 'undefined') {
      spec.alarmAuthData.user = authData.user;
      uAADa = true;
    }
    if (typeof authData.pass !== 'undefined') {
      spec.alarmAuthData.pass = authData.pass;
      uAADa = true;
    }
    if (updAll && uAADa) {
      for (uAADi = 0; uAADi < alarms.length; uAADi++) {
        alarms[uAADi].setAuthData(spec.alarmAuthData);
      }
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

  /**
   * A flag to indicate whether this point has an associated alarm,
   * so that when this point expires, the server should check for
   * alarm states.
   * @type {boolean}
   */
  var alarmAssociated = false;

  // Our private methods.
  /**
   * Checks to see if the passed parameter is a number.
   * @param {something} cValue The parameter which should be checked for
   *                           numberonomy.
   */
  var isNumeric = function(cValue) {
    return !isNaN(parseFloat(cValue)) && isFinite(cValue);
  };

  /**
   * Add ourselves to our server's update list.
   * @param {boolean} force A flag to force the update request.
   */
  var requestUpdate = function(force) {
    // Check if we should be requesting an update yet.
    force = force || false;
    if (that.isTimeSeries() === true &&
        that.timeSeriesInitialised() === false &&
        force === false) {
      // We're not ready for updates yet.
      return;
    }
    // Check that we don't already have all the data we need;
    // this happens when we show archival data and the time-series
    // has already been initialised.
    if (that.timeSeriesInitialised() === true &&
        spec.timeSeriesOptions.startTime !== -1) {
      // No updating for us.
      return;
    }
      
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
   * @param {number} minInterval The minimum value the update interval can be.
   */
  var calcUpdateInterval = function(minInterval) {
	  minInterval = minInterval || 0;
    // Continue only if we're a time-series.
    if (spec.isTimeSeries === true) {
      pointUpdateTime = spec.timeSeriesOptions.spanTime * 60 /
				spec.timeSeriesOptions.maxPoints; // in seconds
	    if (pointUpdateTime < minInterval) {
		    pointUpdateTime = minInterval;
	    }

      // Immediately change the timer if it is already running.
      if (isUpdating === true) {
				configureTimer();
      }
    }
  };
  // We call this now if we are being initialised as a time-series.
  if (spec.isTimeSeries === true) {
	  var minInterval = 0;
	  if (typeof spec.timeSeriesOptions.minimumUpdateInterval !== 'undefined') {
	    minInterval = spec.timeSeriesOptions.minimumUpdateInterval;
	  }
    calcUpdateInterval(minInterval);
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
      var tv = pointValues[l].getValue(gOptions);
      if (typeof gOptions.timeRange !== 'undefined') {
        // Check that the value is in the requested time range.
        if (typeof gOptions.timeRange.min !== 'undefined' &&
            isNumeric(gOptions.timeRange.min) &&
            (gOptions.arrayHighcharts === true &&
             tv[0] < gOptions.timeRange.min) ||
            (gOptions.arrayDojo === true &&
             tv.x < gOptions.timeRange.min)) {
          continue;
        }
        if (typeof gOptions.timeRange.max !== 'undefined' &&
            isNumeric(gOptions.timeRange.max) &&
            (gOptions.arrayHighcharts === true &&
             tv[0] > gOptions.timeRange.max) ||
            (gOptions.arrayDojo === true &&
             tv.x > gOptions.timeRange.max)) {
          continue;
        }
      }
      tSArray.push(tv);
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

    var minInterval = 0;
    if (typeof tsOptions.minimumUpdateInterval !== 'undefined') {
	    minInterval = tsOptions.minimumUpdateInterval;
	  }
    // Recalculate the update interval with the new values.
    calcUpdateInterval(minInterval);

    // We need to get our data again.
    initTimeSeries = false;
    requestUpdate(true);
    
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

  /**
   * Associate or deassociate this point with an alarm, or return the current
   * state.
   * @param {boolean} isAlarm Whether this point is associated with an alarm.
   */
  that.isAlarm = function(isAlarm) {
	  if (typeof isAlarm === 'undefined') {
	    // Return the state.
	    return alarmAssociated;
	  }
	  if (isAlarm) {
	    alarmAssociated = true;
	  } else {
	    alarmAssociated = false;
	  }

	  // Return the new value.
	  return alarmAssociated;
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
    if (dojo.isString(bValue)) {
      return bValue.replace(/[\u00c2><]/g, '');
    }
    return bValue;
  };

  /**
   * Checks to see if the passed parameter is a number.
   * @param {something} cValue The parameter which should be checked for
   *                           numberonomy.
   */
  var isNumeric = function(cValue) {
    return !isNaN(parseFloat(cValue)) && isFinite(cValue);
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
      if (dojo.isString(alteredValue.value)) {
				alteredValue.value = alteredValue.value.sexagesimalToDecimal();
      }
    }

    if ((typeof getOptions.timeAsSeconds !== 'undefined' &&
	       getOptions.timeAsSeconds === true) ||
	      (typeof getOptions.timeAsDateObject !== 'undefined' &&
	       getOptions.timeAsDateObject === true)) {
	    var tels = /^(....)-(..)-(..)_(..):(..):(..)$/.exec(alteredValue.time);
	    if (tels[0] !== '') {
		    // Make the Date object.
		    var timeObj = new Date();
		    timeObj.setUTCFullYear(tels[1], tels[2] - 1, tels[3]);
		    timeObj.setUTCHours(tels[4], tels[5], tels[6], 0);
		    if (getOptions.timeAsDateObject) {
		      alteredValue.time = timeObj;
		    } else if (getOptions.timeAsSeconds) {
		      alteredValue.time = (timeObj.valueOf())/1000;
		    }
	    }
	  }
        
    if (typeof getOptions.referenceValue !== 'undefined' &&
        isNumeric(getOptions.referenceValue)) {
      // Compare this value to a reference value, if it is numeric.
      if (isNumeric(alteredValue.value)) {
        alteredValue.value = alteredValue.value - getOptions.referenceValue;
      }
    }

    if (typeof getOptions.referenceTime !== 'undefined' &&
        isNumeric(getOptions.referenceTime)) {
      // Compare this time to a reference time.
      alteredValue.time = alteredValue.time - getOptions.referenceTime;
    }
        
    // We deal with any requests for how the value should be returned
    // from here.
    if (typeof getOptions.arrayHighcharts !== 'undefined' &&
				getOptions.arrayHighcharts === true) {
      // Return an array with the time as element 0, value as 1
      return ([alteredValue.time, alteredValue.value]);
    } else if (typeof getOptions.arrayDojo !== 'undefined' &&
               getOptions.arrayDojo === true) {
      return {
        x: alteredValue.time,
        y: alteredValue.value
      };
    }

    // If we get here, we've failed, so we just return our value.
    return alteredValue;
  };

  // Return our object to the caller.
  return that;
};

/**
 * This function returns an object pertaining to a single MoniCA alarm
 * point.
 * @param {object} spec Specifications for the object setup.
 * @param {object} my An object reference that this object should be able
 *                    to access.
 */
var monicaAlarm = function(spec, my) {

	/**
	 * The object that we will return to our caller.
	 * @type {object}
	 */
  var that = {};

  /**
	 * The monicaServer object that we are attached to comes in as 'my'.
	 * @type {object}
	 */
  var serverObj = my;

  /**
	 * A list of callback routines to execute when we get updated.
	 * @type {array}
	 */
  var callbacks = [];

  // Check for necessary values in the spec object.
  if (typeof spec === 'undefined' ||
      typeof spec.pointName === 'undefined' ||
	    typeof spec.priority === 'undefined' ||
	    typeof spec.isAlarmed === 'undefined' ||
	    typeof spec.acknowledged === 'undefined' ||
	    typeof spec.acknowledgedBy === 'undefined' ||
	    typeof spec.acknowledgedAt === 'undefined' ||
	    typeof spec.shelved === 'undefined' ||
	    typeof spec.shelvedBy === 'undefined' ||
	    typeof spec.shelvedAt === 'undefined' ||
	    typeof spec.guidance === 'undefined') {
	  return null;
	}

  // Our state.
  var alarmState;

  // Our authentication data.
  var authenticationData = {
    user: '',
	  pass: ''
	};

  // The point we are associated with.
  var associatedPoint = null;

  // Do we need to call our callbacks when we turn off the alarm?
  var callbacksFired;
      
  // Some variables required by our routines.
  var callbackAdded, uSi, aCi, aCfn, fCi, tRet;

  // Our private methods.
  /**
	 * Initialise the object upon receipt of the data from the server.
	 */
  var init = function() {
    alarmState = dojo.clone(spec);

	  // Add the point into the server.
	  tRet = serverObj.addPoints([ alarmState.pointName ]);
    associatedPoint = tRet[0];
    associatedPoint.isAlarm(true);
    callbacksFired = false;
  };
  init();

  // Our public methods.
  /**
	 * Get the current state.
	 */
  that.getState = function() {
    return dojo.clone(alarmState);
	};

  /**
	 * Update the alarm state. This routine will normally be called only
	 * by the server object.
	 * @param {object} nState The new state of the alarm.
	 */
  that.updateState = function(nState) {
    if (typeof nState.pointName === 'undefined' ||
		    nState.pointName !== alarmState.pointName) {
		  // Don't go any further.
		  return;
	  }

	  if (typeof nState.isAlarmed !== 'undefined') {
		  alarmState.isAlarmed = nState.isAlarmed;
	  }
    if (typeof nState.acknowledged !== 'undefined') {
		  alarmState.acknowledged = nState.acknowledged;
	  }
    if (typeof nState.acknowledgedBy !== 'undefined') {
		  alarmState.acknowledgedBy = nState.acknowledgedBy;
	  }
    if (typeof nState.acknowledgedAt !== 'undefined') {
		  alarmState.acknowledgedAt = nState.acknowledgedAt;
	  }
    if (typeof nState.shelved !== 'undefined') {
		  alarmState.shelved = nState.shelved;
	  }
    if (typeof nState.shelvedBy !== 'undefined') {
		  alarmState.shelvedBy = nState.shelvedBy;
	  }
    if (typeof nState.shelvedAt !== 'undefined') {
		  alarmState.shelvedAt = nState.shelvedAt;
	  }

    // Trigger the callbacks if there are any.
    that.fireCallbacks();
  };

  /**
   * Fire the callbacks only if required.
   */
  that.fireCallbacks = function() {
    if (callbacksFired === false) {
      for (fCi = 0; fCi < callbacks.length; fCi++) {
        if (dojo.isFunction(callbacks[fCi])) {
            callbacks[fCi](that);
        }
      }
      callbacksFired = true;
    }
  };
      
  /**
	 * Acknowledge the alarm.
	 * @param {object} authData The username and password object that should
	 *                          be used for this call.
	 */
  that.acknowledge = function(authData) {
    authData = authData || authenticationData;
    authData.user = authData.user || authenticationData.user;
	  authData.pass = authData.pass || authenticationData.pass;
    serverObj.acknowledgeAlarm({
        point: alarmState.pointName,
        value: 'true',
        user: authData.user,
        pass: authData.pass
    });
	};

  /**
	 * Unacknowledge the alarm.
	 * @param {object} authData The username and password object that should
	 *                          be used for this call.
	 */
  that.unacknowledge = function(authData) {
    authData = authData || authenticationData;
    authData.user = authData.user || authenticationData.user;
	  authData.pass = authData.pass || authenticationData.pass;
    serverObj.acknowledgeAlarm({
      point: alarmState.pointName,
      value: 'false',
      user: authData.user,
      pass: authData.pass
    });
	};
      
  /**
	 * Acknowledge or decknowledge the alarm, depending on our internal
   * current state.
	 * @param {object} authData The username and password object that should
	 *                          be used for this call.
	 */
  that.autoAcknowledge = function(authData) {
    if (!alarmState.acknowledged) {
      that.acknowledge(authData);
    } else {
      that.unacknowledge(authData);
    }
	};

  /**
	 * Shelve the alarm.
	 * @param {object} authData The username and password object that should
	 *                          be used for this call.
	 */
  that.shelve = function(authData) {
    authData = authData || authenticationData;
    authData.user = authData.user || authenticationData.user;
	  authData.pass = authData.pass || authenticationData.pass;
    serverObj.shelveAlarm({
      point: alarmState.pointName,
      value: 'true',
      user: authData.user,
      pass: authData.pass
    });
	};

  /**
	 * Unshelve the alarm.
	 * @param {object} authData The username and password object that should
	 *                          be used for this call.
	 */
  that.unshelve = function(authData) {
    authData = authData || authenticationData;
    authData.user = authData.user || authenticationData.user;
	  authData.pass = authData.pass || authenticationData.pass;
    serverObj.shelveAlarm({
      point: alarmState.pointName,
      value: 'false',
      user: authData.user,
      pass: authData.pass
    });
	};

  /**
	 * Shelve or unshelve the alarm, depending on our internal
   * current state.
	 * @param {object} authData The username and password object that should
	 *                          be used for this call.
	 */
  that.autoShelve = function(authData) {
    if (!alarmState.shelved) {
      that.shelve(authData);
    } else {
      that.unshelve(authData);
    }
	};

  /**
	 * Set the username and password for future acknowledge/shelve calls.
	 * @param {object} authData The username and password object that should
	 *                          be used for future calls to acknowledge or
	 *                          shelve this alarm.
	 */
  that.setAuthData = function(authData) {
    if (typeof authData.user !== 'undefined') {
		  authenticationData.user = authData.user;
	  }
	  if (typeof authData.pass !== 'undefined') {
		  authenticationData.pass = authData.pass;
	  }
	  // Return our object for method chaining.
	  return that;
	};

  /**
	 * Add a callback routine to our list.
	 * @param {function} fn The reference to a callback function we are to
	 *                      execute each time we are updated.
	 */
  that.addCallback = function(fn) {
    callbackAdded = false;
	  for (aCi = 0; aCi < callbacks.length; aCi++) {
		  if (fn === callbacks[aCi]) {
		    callbackAdded = true;
		    break;
		  }
	  }
	    
	  if (callbackAdded === false) {
		  callbacks.push(fn);
	  }
	};

  /**
	 * Return the reference to the associated point.
	 */
  that.getPoint = function() {
    return associatedPoint;
	};

  /**
   * Turn the alarm off in anticipation of a new state being returned.
   */
  that.alarmOff = function() {
    alarmState.isAlarmed = false;
    callbacksFired = false;
  };
      
  // Return our object to the caller.
  return that;
};

String.prototype.sexagesimalToDecimal = function(options) {
	// Default options.
	options = options || {};
	if (typeof options.units === 'undefined') {
		options.units = 'degrees';
	};

	// Check for the sign of the number.
	var sign = 1;
	// Remove any plus sign at the start of the string.
	var h = this.replace(/^\+/, '');
  h = h.replace(/[\?\u00B0\']/g, ':');
  h = h.replace(/\"/g, '');
	if (/^\-/.test(h)) {
		// The string appears to be a negative number.
		sign = -1;
	}
	if (sign === -1) {
		// We need to get rid of the negative sign at the front of the string.
		h = h.replace(/^\-/, '');
	}

  
	// Break it into the component elements.
	if (/^\d+h\d+m[\d\.]+s*$/.test(h) === true) {
		// The string is in hours.
		options.units = 'hours';
		h = h.replace(/[hm]/g, ' ');
		h = h.replace(/s/, '');
	} else if (/^\d+d\d+m[\d\.]+s*$/.test(h) === true) {
		// The string is in degrees.
		options.units = 'degrees';
		h = h.replace(/[dm]/g, ' ');
		h = h.replace(/s/, '');
	}
	var hexaSplit = h.split(/[\s\:]/g);
		
	// Check that we only get 3 elements.
	if (hexaSplit.length !== 3) {
		// The string doesn't look right.
		return (!isNaN(parseFloat(this))) ? parseFloat(this) : this;
	}
	// And that each element consists only of numerals.
	for (var i = 0; i < hexaSplit.length; i++) {
		if (/^[\d\.]+$/.test(hexaSplit[i]) === false) {
			// A non-numeric value showed up.
			return this;
		}
	}
		
	// Replace any leading zeroes in the split components.
	for (i = 0; i < hexaSplit.length; i++) {
		if (hexaSplit[i] !== '0') {
			hexaSplit[i] = hexaSplit[i].replace(/^0/, '');
		}
	}
		
	// Convert it into decimal now.
	var decimal = parseInt(hexaSplit[0]) +
		parseInt(hexaSplit[1])/60 +
		parseFloat(hexaSplit[2])/3600;
		
	// Correct the sign now.
	decimal *= sign;
		
	// Convert it into turns now.
	return decimal;
};
