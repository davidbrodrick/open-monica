/*
  open-monica web client Javascript client library
  Copyright 2011 Jamie Stevens

  GPL licence goes here

 */

/**
 * @fileoverview This file contains all the Javascript objects required
 * to easily interface with the open-monica (hereafter MoniCA) server.
 * It requires the Dojo Javascript toolkit (http://dojotoolkit.org) and
 * the date time picker library.
 * @author ste616@gmail.com (Jamie Stevens)
 */

//our Dojo requirements
dojo.require('dijit.Dialog');
dojo.require('dojo.parser');
dojo.require('dojo.NodeList-traverse');
dojo.require('dojo.data.ItemFileReadStore');
dojo.require('dijit.Tree');
dojo.require('dojox.timing._base');



/**
 * Class to interact with the MoniCA server (or at least with the Perl
 * interface on the server).
 * @param {string} host The name of the MoniCA server. It needs to be
 *     a machine that can be accessed from the web server.
 * @class A MoniCA server connection
 * @constructor
 */
function monicaConnection(host) {

  if (!host) {
    /**
     * The MoniCA server to connect to.
     * @type {string}
     */
    this.monitoringServer = 'monhost-nar';
  } else {
    this.monitoringServer = host;
  }

  /**
   * The names of points to get single, latest values for.
   * @type {string}
   */
  this.requestedPoints = [];

  /**
   * The number of times each point in requestedPoints is
   * requested; required so we don't completely remove a point
   * that is being used by multiple children.
   * @type {int}
   */
  this.pointRequests = [];

  /**
   * The data that has been returned from a polling request,
   * chopped up into an array by splitting on newlines.
   * @type {string}
   */
  this.returnedData = [];

  /**
   * The names of the points to get several values for between
   * two specified times.
   * @type {string}
   */
  this.intervalPoints = [];

  /**
   * A pointer to the object that has requested each point.
   * @type {pointer}
   */
  this.intervalPointRequester = [];

  /**
   * The time to start from when requesting the time series
   * points. Should either be -1 for "end now", or a time
   * formatted as "yyyy-mm-dd:hh:mm:ss".
   * @type {Time}
   */
  this.intervalStartTimes = [];

  /**
   * The amount of time the request for time series data should
   * span, in minutes.
   * @type {Interval}
   */
  this.intervalTimeInterval = [];

  /**
   * The maximum number of points that should be returned per
   * query that the server returns. Values greater than a couple
   * of hundred should be avoided else the speed of the request
   * will suffer.
   * @type {int}
   */
  this.intervalMaxPoints = [];

  /**
   * A flag to indicate whether a time series between two fixed
   * times (ie. doesn't "end now") has been completed.
   * @type {boolean}
   */
  this.intervalCompleted = [];

  /**
   * A JSON object that was returned by the server with the data
   * needed for the requested time series.
   * @type {object}
   */
  this.intervalPointResponse = [];

  /**
   * A list of all the monitoring points available from the
   * MoniCA server.
   * @type {string}
   */
  this.availableMonitoringPoints = [];

  /**
   * An object containing the metadata about each requested
   * monitoring point, including their human-readable description,
   * their update times, and their associated units.
   * @type {object}
   */
  this.monPoints = {};

  /**
   * The time between queries to the MoniCA server for new values.
   * This should be a value in ms.
   * @type {int}
   */
  this.updateTime = -1;

  /**
   * A Dojo timing class that we use to trigger our updates.
   * @type {Timer}
   */
  this.updateInterval = new dojox.timing.Timer();

  /**
   * A flag to indicate whether we have been asked to commence
   * periodic updating.
   * @type {boolean}
   */
  this.isUpdating = false;

  // initialise the connection
  this.initialise();
}


/**
 * Disconnect the MoniCA object cleanly, and try to free
 * its memory.
 */
monicaConnection.prototype.disconnect = function() {
  for (var i in this) {
    delete this[i];
  }
};


/**
 * Communicate with the attached MoniCA server.
 * @param {string} action The type of request to make to the server.
 * @param {string} instructions The arguments required by the server to
 *                              successfully complete the request.
 */
monicaConnection.prototype.comms = function(action, instructions) {

  // keep a pointer to our parent object
  var parentThis = this;
  dojo.xhrPost({
    url: '/cgi-bin/obstools/web_monica/monicainterface.pl',
    sync: true,
    content: {action: action, server: this.monitoringServer,
      points: instructions},
    load: function(data, ioargs) {
      parentThis.returnedData = data.split(/\n/);
    },
    error: function(error, ioargs) {
      alert(error);
    }
  });
};


/**
 * Initialise this connection, and obtain a list of all the
 * monitoring points available from the MoniCA server. It is
 * also responsible for assembling the tree menu that presents
 * all the points in an heirarchical list.
 *
 */
monicaConnection.prototype.initialise = function() {

  this.comms('names');

  /**
   * An object to hold the heirarchical list of points
   * as a tree menu.
   * @type {object}
   */
  this.oMPTree = [];
  var thisCount = 0;
  for (var i = 0; i < this.returnedData.length; i++) {
    this.availableMonitoringPoints[i] = this.returnedData[i];
    this.monPoints[this.returnedData[i]] = {};
    // construct the object
    var heirarchy = this.availableMonitoringPoints[i].split(/\./);
    var searchArea = this.oMPTree;
    for (var j = 1; j < heirarchy.length; j++) {
      var isFound = 0;
      // search for an existing element
      for (var k = 0; k < searchArea.length; k++) {
        if (searchArea[k].label == heirarchy[j]) {
          isFound = 1;
          searchArea = searchArea[k].children;
          break;
        }
      }
      if (isFound == 0) {
        // need to create this level
        searchArea.push({id: ++thisCount, label: heirarchy[j],
          children: [] });
        searchArea = searchArea[searchArea.length - 1].children;
      }
    }
    searchArea.push({id: this.availableMonitoringPoints[i], label: heirarchy[0],
      pointName: this.availableMonitoringPoints[i]});
  }

  // reset the list of requested points back to empty
  this.requestedPoints = [];

  // prepare the dojo tree model for all the children
  /**
   * A Dojo data store containing the proper layout of items
   * required to make a Dojo tree menu.
   * @type {ItemFileReadStore}
   */
  this.store = new dojo.data.ItemFileReadStore({
    data: { identifier: 'id', label: 'label', items: this.oMPTree }});

  /**
   * A Dojo tree store containing the data store formatted above.
   * @type {ForestStoreModel}
   */
  this.treeModel = new dijit.tree.ForestStoreModel({ store: this.store });

};


/**
 * Get the metadata for each of the points that have been
 * requested by our children.
 */
monicaConnection.prototype.getDescriptions = function() {

  var wantPoints = '';
  for (var i = 0; i < this.requestedPoints.length; i++) {
    if (i > 0) {
      wantPoints += ';';
    }
    wantPoints += this.requestedPoints[i];
  }
  // get the time series points as well
  for (var j = 0; j < this.intervalPoints.length; j++) {
    if ((i > 1) || (j > 0)) {
      wantPoints += ';';
    }
    wantPoints += this.intervalPoints[j];
  }

  this.comms('descriptions', wantPoints);

  for (var i = 0; i < this.returnedData.length; i++) {
    var descriptionArray = this.returnedData[i].split(/\|/);
    this.monPoints[descriptionArray[0]].pointName = descriptionArray[0];
    this.monPoints[descriptionArray[0]].updateTime = descriptionArray[1];
    this.monPoints[descriptionArray[0]].units = descriptionArray[2];
    this.monPoints[descriptionArray[0]].description = descriptionArray[3];
  }

  // call pollValues now
  this.pollValues();

};


/**
 * Get the current values of all the requested single-value
 * points in requestedPoints.
 */
monicaConnection.prototype.pollValues = function() {

  // construct the points string
  if (this.requestedPoints.length > 0) {
    var wantPoints = '';
    for (var i = 0; i < this.requestedPoints.length; i++) {
      if (i > 0) {
        wantPoints += ';';
      }
      wantPoints += this.requestedPoints[i];
    }
    this.comms('points', wantPoints);

    // process the returned data
    for (var i = 0; i < this.returnedData.length; i++) {
      var dataElements = this.returnedData[i].split(/\s+/);
      if (dataElements[0] != '') {
        this.updateClass(dataElements[0], dataElements[2], dataElements[3],
            this.monPoints[dataElements[0]].description,
            this.monPoints[dataElements[0]].units);
      }
    }
  }

  // get the time series values
  if (this.intervalPoints.length > 0) {
    var wantPoints = '';
    var donePoints = 0;
    for (var i = 0; i < this.intervalPoints.length; i++) {
      if (this.intervalCompleted[i] == false) {
        if (donePoints > 0) {
          wantPoints += ';';
        }
        wantPoints += this.intervalPoints[i] + ',' +
            this.intervalStartTimes[i] +
            ',' + this.intervalTimeInterval[i] + ',' +
            this.intervalMaxPoints[i];
        donePoints++;
      }
    }
    this.comms('intervals', wantPoints);

    // process the returned data
    for (var i = 0; i < this.returnedData.length; i++) {
      if (this.returnedData[i] != '') {
        // removes silly symbol coming with some phases
        this.returnedData[i] = this.returnedData[i].replace(/[\u00c2><]/g, '');
        this.intervalPointResponse[i] = eval('(' + this.returnedData[i] + ')');
        this.intervalPointResponse[i] =
            this.convertToNumbers(this.intervalPointResponse[i]);
      }
    }
    // tell our requesters to update
    var requestersTold = [];
    for (var i = 0; i < this.intervalPointRequester.length; i++) {
      // has it already been triggered?
      var triggered = 0;
      for (var j = 0; j < requestersTold.length; j++) {
        if (requestersTold[j] == this.intervalPointRequester[i]) {
          triggered = 1;
          break;
        }
      }
      if (triggered == 0) {
        // do the triggering
        this.intervalPointRequester[i].updaterFunction();
        requestersTold.push(this.intervalPointRequester[i]);
      }
    }
  }

};


/**
 * Checks if a value passed to it is a finite number.
 * @param {anything} n The variable that you want checked.
 * @return {boolean} True if the variable is a finite number.
 */
function isNumber(n) {
  return typeof n === 'number' && isFinite(n);
}


/**
 * Takes some JSON data from the MoniCA server response, and
 * converts any strings that look like DD:MM:SS into a decimal
 * number.
 * @param {object} JSONdata JSON data from the MoniCA server.
 * @return {object} The same JSON data where any DD:MM:SS strings
 *                   have been converted into decimal numbers.
 */
monicaConnection.prototype.convertToNumbers = function(JSONdata) {

  for (var i = 0; i < JSONdata.data.length; i++) {
    if (isNumber(JSONdata.data[i][1])) {
      continue;
    }
    if ((JSONdata.data[i][1].match(/\:/)) ||
        (JSONdata.data[i][1].match(/\|/))) {
      // it's a DMS-type value
      var els = JSONdata.data[i][1].split(/[\:\|]/g);
      var sign = 1;
      if (JSONdata.data[i][1].match(/^\-/)) {
        sign = -1;
      }
      els[0] *= sign;
      var newValue = els[0] + els[1] / 60 + els[2] / 3600;
      newValue *= sign;
      JSONdata.data[i][1] = newValue;
    }
  }

  return JSONdata;
};


/**
 * Updates a web page with appropriate markup to contain the
 * descriptions, units and values of requested single-value
 * MoniCA points.
 * The web page should have a DOM element that has a class name
 * of the MoniCA point name with the full stops (.) removed,
 * and optionally prepended with "description_" or "units_".
 * For example, to have this routine update a DOM element with
 * the value of the MoniCA point "site.environment.weather.Temperature",
 * it should have the class "siteenvironmentweatherTemperature". To
 * have this routine update a DOM element with the human-readable
 * description of the same point, it should have the class
 * "description_siteenvironmentweatherTemperature", or
 * "units_siteenvironmentweatherTemperature" if it should be updated
 * with the point's units.
 * @param {string} targetClass The name of the MoniCA point, with the
 *                             full stops included.
 * @param {value} newValue The latest value for the specified MoniCA
 *                         point.
 * @param {boolean} pointOK Flag to indicate whether the point is in
 *                          an error state; true for "no error".
 * @param {string} classDescription The human-readable description for
 *                                  the specified point.
 * @param {string} classUnits The units associated with the specified
 *                            point.
 */
monicaConnection.prototype.updateClass =
    function(targetClass, newValue, pointOK, classDescription, classUnits) {

  var searchClassName = this.safifyClassName(targetClass);
  var descriptionClassName = 'description_' + searchClassName;
  var unitsClassName = 'units_' + searchClassName;

  // clean up the value
  // removes silly symbol coming with some phases
  newValue = newValue.replace(/[\u00c2><]/g, '');

  dojo.query('.' + searchClassName).forEach(function(node, index, arr) {
    if (dojo.isIE) {
      // god, IE is so fracking stupid
      node.innerText = newValue;
    } else {
      node.innerHTML = newValue;
    }
    if (pointOK == 'true') {
      dojo.removeClass(node, 'pointWarning');
    } else {
      dojo.addClass(node, 'pointWarning');
    }
  });
  dojo.query('.' + descriptionClassName).forEach(function(node, index, arr) {
    if (dojo.isIE) {
      node.innerText = classDescription;
    } else {
      node.innerHTML = classDescription;
    }
  });
  dojo.query('.' + unitsClassName).forEach(function(node, index, arr) {
    if (dojo.isIE) {
      node.innerText = classUnits;
    } else {
      node.innerHTML = classUnits;
    }
  });
};


/**
 * This routine takes a MoniCA point name and converts it into
 * a class name by removing any full stops (.) and by replacing
 * any "+" symbols with the word "plus".
 * @param {string} inputClass The name of the MoniCA point.
 * @return {string} The MoniCA point name with the full stops
 *                   and "+" symbols altered.
 */
monicaConnection.prototype.safifyClassName = function(inputClass) {
  var stage1 = inputClass.replace(/\./g, '');
  var stage2 = stage1.replace(/\+/g, 'plus');
  return (stage2);
};


/**
 * Add one or more points to the single-value point query list.
 * @param {array} points A list of MoniCA point names to add.
 */
monicaConnection.prototype.addUpdatePoint = function(points) {

  // cycle through the points we've been asked to add
  for (var i = 0; i < points.length; i++) {
    // check that the point isn't already there
    var pointExists = -1;
    for (var j = 0; j < this.requestedPoints.length; j++) {
      if (points[i] == this.requestedPoints[j]) {
        pointExists = j;
        break;
      }
    }
    if (pointExists == -1) {
      // add the point to the update list
      this.requestedPoints.push(points[i]);
      this.pointRequests.push(1);
    } else {
      this.pointRequests[j]++;
    }
  }

};


/**
 * Add one or more points to the time series point query list.
 * @param {array} points A list of MoniCA point names to add.
 * @param {pointer} requestObject A pointer to the object that is
 *                                making the request.
 */
monicaConnection.prototype.addTimeSeriesPoint = function(points, 
    requestObject) {

  // cycle through the points we've been asked to add
  for (var i = 0; i < points.length; i++) {
    // see if this point has already been requested by this object
    var pointExists = -1;
    for (var j = 0; j < this.intervalPoints.length; j++) {
      if ((this.intervalPoints[j] == points[i]) &&
          (this.intervalPointRequester[j] == requestObject)) {
        pointExists = j;
        break;
      }
    }
    if (pointExists == -1) {
      // add the point to the update list
      this.intervalPoints.push(points[i]);
      this.intervalPointRequester.push(requestObject);
      this.intervalStartTimes.push(null);
      this.intervalTimeInterval.push(null);
      this.intervalMaxPoints.push(null);
      this.intervalCompleted.push(false);
    }
  }

};


/**
 * Remove one or more points from the time series point query
 * list.
 * @param {array} points A list of MoniCA point names to remove.
 * @param {pointer} requestObject A pointer to the object that is
 *                                making the request.
 */
monicaConnection.prototype.removeTimeSeriesPoint = function(points,
    requestObject) {

  // are we removing specific points
  if (points != null) {
    // cycle through the points
    for (var i = 0; i < points.length; i++) {
      // find the point
      for (var j = 0; j < this.intervalPoints.length; j++) {
        if ((this.intervalPoints[j] == points[i]) &&
            (this.intervalPointRequester[j] == requestObject)) {
          // remove this point
          this.intervalPoints.splice(j, 1);
          this.intervalPointRequester.splice(j, 1);
          this.intervalStartTimes.splice(j, 1);
          this.intervalTimeInterval.splice(j, 1);
          this.intervalMaxPoints.splice(j, 1);
          this.intervalCompleted.splice(j, 1);
          break;
        }
      }
    }
  } else {
    // remove all the points from this requester
    for (var i = 0; i < this.intervalPoints.length; i++) {
      if (this.intervalPointRequester[i] == requestObject) {
        // remove this point
        this.intervalPoints.splice(i, 1);
        this.intervalPointRequester.splice(i, 1);
        this.intervalStartTimes.splice(i, 1);
        this.intervalTimeInterval.splice(i, 1);
        this.intervalMaxPoints.splice(i, 1);
        this.intervalCompleted.splice(i, 1);
        i--;
      }
    }
  }
};


/**
 * Set the start time, time range and the maximum number of
 * returned values for each point in a time series requested
 * by a specified object.
 * @param {pointer} requestObject A pointer to the object that is
 *                                making the request.
 * @param {time} startTime The start time formatted as
 *                         'yyyy-mm-dd:HH:MM:SS', or -1 for "until now".
 * @param {interval} timeInterval The time range to query, in
 *                                minutes.
 * @param {int} maxPoints The maximum number of points to return
 *                        for each server query.
 * @return {boolean} A flag to indicate whether the specified
 *                    settings were different to the existing settings.
 */
monicaConnection.prototype.setTimeSeriesPointRange =
    function(requestObject, startTime, timeInterval, maxPoints) {

  var changedSettings = false;

  // cycle through all the time series points we have
  for (var i = 0; i < this.intervalPointRequester.length; i++) {
    // is this point requester the requester of this point?
    if (this.intervalPointRequester[i] == requestObject) {
      // change the time interval
      if ((this.intervalStartTimes[i] != startTime) ||
          (this.intervalTimeInterval[i] != timeInterval) ||
          (this.intervalMaxPoints[i] != maxPoints)) {
        this.intervalCompleted[i] = false;
        this.intervalStartTimes[i] = startTime;
        this.intervalTimeInterval[i] = timeInterval;
        this.intervalMaxPoints[i] = maxPoints;
        changedSettings = true;
      }
    }
  }

  return changedSettings;
};


/**
 * Return the time series points to the object, as a JSON object,
 * to the object that has requested them.
 * @param {pointer} requestObject The child object that has requested
 *                                time series points.
 * @return {array} An array of JSON objects, each element being one
 *                  of the points that the child object had requested.
 */
monicaConnection.prototype.getTimeSeriesPoints = function(requestObject) {

  var returnedData = [];
  for (var i = 0; i < this.intervalPoints.length; i++) {
    if (this.intervalPointRequester[i] == requestObject) {
      if (this.intervalPointResponse[i]) {
        returnedData.push(this.intervalPointResponse[i]);
      }
    }
  }

  return returnedData;
};


/**
 * Remove one or more points from the single-value point query
 * list.
 * @param {array} points A list of MoniCA point names to remove.
 */
monicaConnection.prototype.removeUpdatePoint = function(points) {

  // if we don't have any requested points, simply return
  if (!this.requestedPoints) {
    return;
  }

  // cycle through the points we've been asked to remove
  for (var i = 0; i < points.length; i++) {
    // find the point
    for (var j = 0; j < this.requestedPoints.length; j++) {
      if (points[i] == this.requestedPoints[j]) {
        this.pointRequests[j]--;
        if (this.pointRequests[j] == 0) {
          // nobody else wants this point
          this.requestedPoints.splice(j, 1);
          this.pointRequests.splice(j, 1);
        }
        break;
      }
    }
  }
};


/**
 * Start periodically querying the MoniCA server for the
 * values for requested points.
 * @param {int} period The period, in seconds, between successive
 *                     queries.
 */
monicaConnection.prototype.startMonitoring = function(period) {
  // stop any current intervals
  this.updateInterval.stop();
  this.isUpdating = false;

  // period should be in seconds, we'll convert it to ms here
  if (!period) {
    // use the current period
    period = this.updateTime;
  } else {
    // set the interval
    this.updateTime = period * 1000;
  }
  this.updateInterval.setInterval(this.updateTime);
  var parentThis = this;
  this.updateInterval.onTick = function() {
    parentThis.pollValues();
  }
  this.updateInterval.start();
  this.isUpdating = true;
};


/**
 * Stop periodically querying the MoniCA server.
 */
monicaConnection.prototype.stopMonitoring = function() {
  // stop any current intervals
  this.updateInterval.stop();
  this.isUpdating = false;
};



/**
 * Class that makes a new DOM node, and presents a table
 * that contains both a "pane" for showing a MoniCA point table
 * or time series plot (or anything else really), and a "pane"
 * for allowing the editing of the table/plot. This class handles
 * the transition between the content and edit panes via a button,
 * and also presents a "close" button that gracefully destroys both
 * our DOM node and its children.
 * @param {object} options A list of options to configure the
 *                         container.
 * @constructor
 */
function monicaContainer(options) {

  /**
   * The name of this container.
   * @type {string}
   */
  this.containerName = '';

  /**
   * The parent node of this container.
   * @type {DOMNode}
   */
  this.containerParent = null;

  if (options) {
    if (options.name) {
      this.containerName = options.name;
    }
    if (options.parent) {
      this.containerParent = options.parent;
    }
  }

  // make a new global div
  /**
   * The DOM node that this container uses as its top.
   * @type {DOMNode}
   */
  this.domNode = dojo.create('div');
  dojo.attr(this.domNode, 'class', 'monicaContainer');
  if (this.containerName != '') {
    this.domNode.id = this.containerName;
  }

  // make this div moveable to anywhere on the page
  //    this.dnd=new dojo.dnd.Moveable(this.domNode);

  /**
   * A button at the top of the container that allows the user
   * to switch between the content and editing panes.
   * @type {DOMNode}
   */
  this.changeViewButton =
      dojo.create('button', {type: 'button', 'class': 'changeViewButton',
        innerHTML: 'Edit'});
  this.domNode.appendChild(this.changeViewButton);

  /**
   * The handle for the event connection for the edit/shown content
   * button on the container.
   * @type {EventHandle}
   */
  this.viewButtonHandle =
      dojo.connect(this.changeViewButton, 'onclick', this, this.switchView);

  // add the container close button
  /**
   * A button at the top right of the container that allows the
   * user to close the container and cleanly destroy all its
   * children.
   * @type {DOMNode}
   */
  this.closeViewImage = dojo.create('img', {src: 'closebutton.png',
    'class': 'closeButton',
    width: '16px', height: '16px'});
  this.domNode.appendChild(this.closeViewImage);

  /**
   * The handle for the event connection for the close container
   * button.
   * @type {EventHandle}
   */
  this.closeButtonHandle =
      dojo.connect(this.closeViewImage, 'onclick', this, this.destroyContainer);

  // make two overlapping containers, one for the content, the other
  // for the editing functions
  /**
   * A table with two rows, one to show the content, one to show
   * the editing functions. We do it this way, and use the row
   * "collapse" visibility to hide/show the rows, because CSS with
   * normal positioning was not working as desired.
   * @type {DOMNode}
   */
  this.overlapTable = dojo.create('table', {'class': 'containerOverlapTable'});

  /**
   * A table row for the content.
   * @type {DOMNode}
   */
  this.contentRow = dojo.create('tr');
  this.overlapTable.appendChild(this.contentRow);

  /**
   * A table row for the editing functions.
   * @type {DOMNode}
   */
  this.editorRow = dojo.create('tr');
  this.overlapTable.appendChild(this.editorRow);

  /**
   * A div to attach all the content nodes to.
   * @type {DOMNode}
   */
  this.content = dojo.create('div', {'class': 'containerContent'});

  /**
   * A div to attach all the editing nodes to.
   * @type {DOMNode}
   */
  this.editor = dojo.create('div', {'class': 'containerEditor'});
  this.contentRow.appendChild(this.content);
  this.editorRow.appendChild(this.editor);
  this.domNode.appendChild(this.overlapTable);

  // set the initial visibilities
  this.contentRow.style.visibility = 'visible';
  this.editorRow.style.visibility = 'collapse';

  /**
   * A flag to indicate whether the container is showing the
   * content or editing nodes; true indicates the content is
   * visible.
   * @type {boolean}
   */
  this.isShowing = true;

  /**
   * A pointer to the child that is hooked onto this container.
   * @type {pointer}
   */
  this.childObject = null;

  /**
   * The type of child that is hooked onto this container.
   * @type {string}
   */
  this.childType = '';
}


/**
 * Destroy the container, and clean up both our own variables
 * and those of our child.
 * @param {object} evtObj The object that triggered the call to
 *                        this function.
 */
monicaContainer.prototype.destroyContainer = function(evtObj) {

  // tell our parent we're closing
  if (this.containerParent) {
    this.containerParent.childStopped(this);
  }

  // close the child object
  this.childObject.destroy();

  // disconnect our buttons
  dojo.disconnect(this.viewButtonHandle);
  dojo.disconnect(this.closeButtonHandle);

  // destroy the domNode
  dojo.destroy(this.domNode);
};


/**
 * Toggle the visibility of the content and editing panes.
 * @param {object} evtObj The object that triggered the call to
 *                        this function.
 */
monicaContainer.prototype.switchView = function(evtObj) {

  if (this.changeViewButton.innerHTML == 'Edit') {
    // switch to the edit view by hiding the content div
    this.changeViewButton.innerHTML = 'Show Content';
    this.contentRow.style.visibility = 'collapse';
    this.editorRow.style.visibility = 'visible';
    this.isShowing = false;
  } else {
    this.changeViewButton.innerHTML = 'Edit';
    this.contentRow.style.visibility = 'visible';
    this.editorRow.style.visibility = 'collapse';
    this.isShowing = true;
    if (this.childObject.hasChanged == true) {
      this.childObject.updaterFunction();
    }
  }
};



//our point table object
/**
 * A MoniCA point table object. It needs to be attached to both
 * a pre-existing monicaConnection object and a monicaContainer
 * object.
 * @param {object} monicaServer A pointer to the MoniCA server
 *                              responsible for updating the points
 *                              in our table.
 * @param {object} monicaContainer A pointer to the MoniCA container
 *                                 that we are attached to.
 * @see monicaConnection
 * @see monicaContainer
 * @constructor
 */
function pointTable(monicaServer, monicaContainer) {

  // the points we are displaying
  /**
   * A list of points that we want to display in our table.
   * @type {string}
   */
  this.hasPoints = [];

  /**
   * The DOM node under which all the point table values will be attached.
   * @type {DOMNode}
   */
  this.contentDomNode = dojo.create('div', {'class': 'pointTableDiv'});

  /**
   * The DOM node under which all the point table editing functions will
   * be attached.
   * @type {DOMNode}
   */
  this.editDomNode = dojo.create('div', {'class': 'pointTableEdit'});

  /**
   * A pointer to the MoniCA server that is responsible for getting the
   * values for the points in our table.
   * @type {pointer}
   */
  this.monicaServer = monicaServer;

  /**
   * A pointer to the MoniCA container that is handling our content and
   * editing panes.
   * @type {pointer}
   */
  this.monicaContainer = monicaContainer;

  // set all the required values in the server and container
  // we are the child object of the container
  this.monicaContainer.childObject = this;
  this.monicaContainer.childType = 'pointTable';

  /**
   * A pointer to the internal method we use to form and update the
   * point table.
   * @type {pointer}
   */
  this.updaterFunction = this.updateTables;

  /**
   * A flag to indicate whether the point table requires change, usually
   * because the user has added or removed a point from it.
   * @type {boolean}
   */
  this.hasChanged = true;

  // append our nodes to the container
  this.monicaContainer.content.appendChild(this.contentDomNode);
  this.monicaContainer.editor.appendChild(this.editDomNode);

  // add the editing functions
  /**
   * The DOM node that contains our tree menu of all available MoniCA points.
   * @type {DOMNode}
   */
  this.treeSideDiv = dojo.create('div', {'class': 'pointTableTreeSideDiv'});
  this.treeSideDiv.appendChild(dojo.create('p',
      {innerHTML: 'Available Points'}));

  /**
   * The parent DOM node of our tree menu.
   * @type {DOMNode}
   */
  this.treeDiv = dojo.create('div', {'class': 'pointTableTreeDiv'});

  /**
   * A Dojo tree menu that is populated with the model of all available MoniCA
   * point names, obtained from our attached MoniCA server.
   */
  this.treeControl = new dijit.Tree({
    model: this.monicaServer.treeModel,
    showRoot: false,
    'class': 'pointTreeControl'});
  this.treeDiv.appendChild(this.treeControl.domNode);
  this.treeSideDiv.appendChild(this.treeDiv);
  this.editDomNode.appendChild(this.treeSideDiv);

  /**
   * An event handle that will be triggered whenever the user double-clicks
   * one of the MoniCA points in the tree menu.
   * @type {eventHandle}
   */
  this.treeHandle = dojo.connect(this.treeControl, 'onDblClick', this,
      this.addFromClick);

  // the multi-select box showing what points we currently show
  /**
   * The DOM node that will contain a multi-select box that will show all
   * the MoniCA point names that are currently being displayed in the point
   * table.
   * @type {DOMNode}
   */
  this.editSelectDiv = dojo.create('div', {'class': 'pointTableEditDiv'});
  this.editSelectDiv.appendChild(dojo.create('p',
      {innerHTML: 'Points in table'}));

  /**
   * The parent DOM node of our multi-select box.
   * @type {DOMNode}
   */
  this.editSelectScrollDiv = dojo.create('div',
      {'class': 'pointTableEditScrollDiv'});
  this.editSelectDiv.appendChild(this.editSelectScrollDiv);

  /**
   * The multi-select box that will show all the MoniCA point names that
   * are currently part of the point table.
   * @type {DOMNode}
   */
  this.editSelect = dojo.create('select', {multiple: 'multiple', size: '20',
    'class': 'pointTableEditSelect'});
  this.editSelectScrollDiv.appendChild(this.editSelect);

  /**
   * A button that will allow the user to remove all the points that they have
   * selected in the multi-select box from the point table.
   * @type {button}
   */
  this.editRemoveButton = dojo.create('button',
      {type: 'button', innerHTML: 'Remove Points',
        'class': 'pointTableEditRemoveButton'});
  this.editSelectDiv.appendChild(this.editRemoveButton);

  /**
   * An event handler that is triggered when the user hits the "Remove Points"
   * button.
   * @type {eventHandle}
   */
  this.removeButtonHandle = dojo.connect(this.editRemoveButton, 'onclick',
      this, this.removePoints);

  /**
   * An event handler that is triggered when the user double-clicks a point
   * in the multi-select box, to remove the point from the point table.
   * @type {eventHandle}
   */
  this.removeClickHandle = dojo.connect(this.editSelect, 'ondblclick', this,
      this.removeFromClick);
  this.editDomNode.appendChild(this.editSelectDiv);

  /**
   * A list of all the MoniCA point names that are being displayed in the
   * point table, formatted to be inserted into the multi-select input
   * box.
   * @type {DOMNodeArray}
   */
  this.editSelectOptions = []; // we don't yet have any points
}


/**
 * Destroy the pointTable by disconnecting our event handlers and requesting
 * that all our points be removed from the update list.
 */
pointTable.prototype.destroy = function() {
  // we need to provide this function for the container to close us correctly

  // get rid of all our points from the MoniCA server
  this.monicaServer.removeUpdatePoint(this.hasPoints);

  // disconnect our buttons
  dojo.disconnect(this.treeHandle);
  dojo.disconnect(this.removeButtonHandle);
  dojo.disconnect(this.removeClickHandle);

};


/**
 * Add a point to the server's update list after the user has double-clicked
 * on a point in the tree menu.
 * @param {object} evtObj The tree menu object that was double-clicked on.
 * @see #addPoints
 */
pointTable.prototype.addFromClick = function(evtObj) {
  // determine the item that was clicked
  var clickedId = this.monicaServer.store.getValue(evtObj, 'id');
  var clickedElements = clickedId.split(/\./);
  // add this element to the points
  var clickedIds = [clickedId];
  this.addPoints(clickedIds);

};


/**
 * Add one or more points to the server's update list via a programmatic
 * request.
 * @param {array} points An array of strings, where each element is the
 *                       name of a MoniCA point we should display the
 *                       value for.
 */
pointTable.prototype.addPoints = function(points) {

  // all the new points will be stored here
  var allNew = [];

  // cycle through the points we've been asked to add
  for (var i = 0; i < points.length; i++) {
    // check that the point isn't already there
    var pointExists = 0;
    for (var j = 0; j < this.hasPoints.length; j++) {
      if (points[i] == this.hasPoints[j]) {
        pointExists = 1;
        break;
      }
    }
    if (pointExists == 0) {
      // add the point to our list
      this.hasPoints.push(points[i]);
      allNew.push(points[i]);
      // add a new option to the select
      this.editSelectOptions.push(dojo.create('option',
          {innerHTML: points[i]}));
      this.editSelect.
          appendChild(this.
              editSelectOptions[this.editSelectOptions.length - 1]);
    }
  }

  // add the points to the update list
  this.monicaServer.addUpdatePoint(allNew);

  this.hasChanged = true;

  // check whether we should be updating now
  if (this.monicaContainer.isShowing == true) {
    this.updaterFunction();
  }

};


/**
 * Remove an object from the server's update list after a user has double-
 * clicked on a point name in the list of currently displayed points.
 * @param {object} eventObj The DOM node of the list of the currently
 *                          displayed points.
 */
pointTable.prototype.removeFromClick = function(eventObj) {
  // determine the item that was clicked
  var clickedSelectOption = eventObj.target;

  // remove from the MoniCA list
  this.monicaServer.removeUpdatePoint([clickedSelectOption.innerHTML]);

  for (var i = 0; i < this.hasPoints.length; i++) {
    if (this.hasPoints[i] == clickedSelectOption.innerHTML) {
      this.hasPoints.splice(i, 1);
      break;
    }
  }
  dojo.destroy(clickedSelectOption);

  // indicate we need an update
  this.hasChanged = true;
};


/**
 * Remove points from our list of displayed points after the user has selected
 * one or more points in the displayed points select box and clicked the
 * "Remove Points" button.
 */
pointTable.prototype.removePoints = function() {

  // find all the currently selected points from the multi-select
  var allToRemove = [];

  for (var i = 0; i < this.editSelectOptions.length; i++) {
    if (this.editSelectOptions[i].selected) {
      allToRemove.push(this.editSelectOptions[i].innerHTML);
      // find this point
      for (var j = 0; j < this.hasPoints.length; j++) {
        if (this.hasPoints[j] == this.editSelectOptions[i].innerHTML) {
          this.hasPoints.splice(j, 1);
          break;
        }
      }
      dojo.destroy(this.editSelectOptions[i]);
      this.editSelectOptions.splice(i, 1);
      i--;
    }
  }

  // remove from the MoniCA list
  this.monicaServer.removeUpdatePoint(allToRemove);

  // indicate we need an update
  this.hasChanged = true;
};


/**
 * Update the format of the point table.
 */
pointTable.prototype.updateTables = function() {

  // empty the current content DOM node
  dojo.empty(this.contentDomNode);

  // make as many tables as we need to
  var tableNodes = [];
  var tableCollection = [];
  var tableColumns = [];
  var tablePoints = [];

  // go through the points and assign tables and columns
  for (var i = 0; i < this.hasPoints.length; i++) {
    // break the point name down
    var pointElements = this.hasPoints[i].split(/\./);
    var pointPrefix = pointElements[1];
    for (var j = 2; j < pointElements.length; j++) {
      pointPrefix += '.' + pointElements[j];
    }
    // what pattern is this point?
    var pointPattern = /^(\D+)(\d*)(.*)$/i.exec(pointElements[0]);
    // check if we need a new collection
    var needNew = 1;
    for (var j = 0; j < tableCollection.length; j++) {
      if (tableCollection[j] == pointPattern[1]) {
        // is it a new column?
        var newColumn = 1;
        for (var k = 0; k < tableColumns[j].length; k++) {
          if (tableColumns[j][k] == pointElements[0]) {
            newColumn = 0;
            break;
          }
        }
        if (newColumn == 1) {
          tableColumns[j].push(pointElements[0]);
        }
        // is it a new point?
        var newPoint = 1;
        for (var k = 0; k < tablePoints[j].length; k++) {
          if (tablePoints[j][k] == pointPrefix) {
            newPoint = 0;
            break;
          }
        }
        if (newPoint == 1) {
          tablePoints[j].push(pointPrefix);
        }
        needNew = 0;
        break;
      }
    }
    if (needNew == 1) {
      tableCollection.push(pointPattern[1]);
      tableColumns.push([pointElements[0]]);
      tablePoints.push([pointPrefix]);
    }
  }

  // make the tables
  for (var i = 0; i < tableCollection.length; i++) {
    // sort the columns
    tableColumns[i].sort();
    // start this table
    tableNodes[i] = dojo.create('table', {'class': 'pointTable'});
    // make the header row
    var headerRow = dojo.create('tr');
    // the first cell is blank
    var headerCell = dojo.create('td');
    headerRow.appendChild(headerCell);
    // the next few cells are for the number of columns
    for (var j = 0; j < tableColumns[i].length; j++) {
      headerCell = dojo.create('td', {innerHTML: '<b>' + tableColumns[i][j] +
            '</b>'});
      headerRow.appendChild(headerCell);
    }
    // the last cell is for the units
    headerCell = dojo.create('td');
    headerRow.appendChild(headerCell);
    // put this row on the table
    tableNodes[i].appendChild(headerRow);
    // go through the points on this table
    for (var j = 0; j < tablePoints[i].length; j++) {
      // start a new row
      var pointRow = dojo.create('tr');
      // get all the points from the full list that have the right prefix
      var pointPoints = [];
      var matchPattern = new RegExp(tablePoints[i][j] + '$');
      for (var k = 0; k < this.hasPoints.length; k++) {
        if (this.hasPoints[k].search(matchPattern) != -1) {
          pointPoints.push(this.hasPoints[k]);
        }
      }
      // get the description from the first point
      var pointCell = dojo.create('th', {'class': 'description_' +
            this.monicaServer.safifyClassName(pointPoints[0]),
        innerHTML: '&nbsp;'});
      pointRow.appendChild(pointCell);
      // now go through the appropriate points and add them to the
      // appropriate columns
      for (var k = 0; k < tableColumns[i].length; k++) {
        pointCell = dojo.create('td', {innerHTML: '&nbsp;'});
        for (var l = 0; l < pointPoints.length; l++) {
          var pointElements = pointPoints[l].split(/\./);
          if (pointElements[0] == tableColumns[i][k]) {
            dojo.addClass(pointCell,
                this.monicaServer.safifyClassName(pointPoints[l]));
            break;
          }
        }
        pointRow.appendChild(pointCell);
      }
      // now the units cell
      pointCell = dojo.create('td', {'class': 'units_' +
            this.monicaServer.safifyClassName(pointPoints[0]),
        innerHTML: '&nbsp;'});
      pointRow.appendChild(pointCell);
      tableNodes[i].appendChild(pointRow);
    }
    this.contentDomNode.appendChild(tableNodes[i]);
  }

  this.monicaServer.getDescriptions();
  this.hasChanged = false;
};



/**
 * Make the standard MoniCA web client "front page", that allows the user
 * to easily create and remove containers, generate links, load presets,
 * and control the connections to MoniCA servers.
 * @param {object} options Options object used to control the appearance
 *                          of the front page.
 * @constructor
 */
function monicaHTMLFrontPage(options) {

  // make sure we have all the options we need
  if (!options) {
    document.write('New monicaHTMLFrontPage was not called with an ' +
        'options object, you fail!');
    return (null);
  }
  if (!options.topDivId) {
    document.write('Must supply ID of top level element to attach to, ' +
        'as topDivId!');
    return (null);
  }
  if (!options.availableServers) {
    document.write('Must supply list of available MoniCA servers, ' +
        'as availableServers!');
    return (null);
  }

  /**
   * The ID of the main document's DOM node that we will attach to.
   * @type {string}
   */
  this.topDivId = options.topDivId;

  /**
   * The list of servers that will be available for the user to select.
   * @type {object}
   */
  this.availableServers = options.availableServers;
  if (!options.updateTime || options.updateTime < 0) {

    /**
     * The time, in seconds, to ask the server for new values.
     * @type {int}
     */
    this.updateTime = 10;
  } else {
    this.updateTime = options.updateTime;
  }

  /**
   * Details about the MoniCA server we are connected to, along with a pointer
   * to the monicaServer object.
   * @type {object}
   */
  this.monicaConnection = { serverIndex: -1, serverConnection: null };

  // check whether we should be making a connection immediately
  for (var i = 0; i < this.availableServers.length; i++) {
    if (this.availableServers[i].connect &&
        this.availableServers[i].connect == true) {
      this.monicaConnection.serverConnection =
          new monicaConnection(this.availableServers[i].host);
      this.monicaConnection.serverIndex = i;
      break; // we'll only connect to the first one
    }
  }

  // who are our child containers?
  /**
   * A list of pointers to the containers that we have created and that still
   * exist on the front page.
   * @type {pointer}
   */
  this.childContainers = [];

  /**
   * A list of pointers to the point tables (inside the container) that we
   * have created and that still exist on the front page.
   * @type {pointer}
   */
  this.childPointTables = [];

  /**
   * A list of pointers to the time series plots (inside the container)
   * that we have created and that still exist on the front page.
   * @type {pointer}
   */
  this.childPlots = [];

  // create a popup window for various purposes
  /**
   * A popup dialog that we will use to show the links generated by the page,
   * and to allow selection of preset containers.
   * @type {dialog}
   */
  this.dialogPopup = new dijit.Dialog({title: 'MoniCA Dialog'});

  /**
   * A DOM node to contain the content for the popup dialog.
   * @type {DOMNode}
   */
  this.dialogPopupDiv = dojo.create('div', {id: 'frontPageDialog'});
  this.dialogPopup.attr('content', this.dialogPopupDiv);

  // we'll use row-hiding tables for this dialog
  /**
   * A table to support more than one overlapping content pane.
   * @type {DOMNode}
   */
  this.dialogTable = dojo.create('table');
  this.dialogPopupDiv.appendChild(this.dialogTable);

  // one purpose is to display the link generated for a particular layout
  /**
   * The row to hold the generated link text content.
   * @type {DOMNode}
   */
  this.dialogLinkRow = dojo.create('tr');
  this.dialogTable.appendChild(this.dialogLinkRow);

  /**
   * The cell to hold the generated link text content.
   * @type {DOMNode}
   */
  this.dialogLinkCell = dojo.create('td');
  this.dialogLinkRow.appendChild(this.dialogLinkCell);
  this.dialogLinkCell.appendChild(dojo.create('div',
      {innerHTML: 'Page link:'}));

  /**
   * A text box that will be used to contain the generated link text.
   * @type {DOMNode}
   */
  this.dialogLinkBox = dojo.create('textarea',
      {name: 'dialogLinkBox', cols: '60', rows: '20'});
  this.dialogLinkCell.appendChild(this.dialogLinkBox);

  /**
   * The DOM node of the front page header bar and menu.
   * @type {DOMNode}
   */
  this.frontPageHeader = null;

  // execute the draw routine
  this.draw();
}


/**
 * Connect to a specified MoniCA server.
 * @param {object} server Details of the server to connect to.
 */
monicaHTMLFrontPage.prototype.connectServer = function(server) {

  // programmatically establish a connection
  // check whether we are already connected to this server
  if (this.monicaConnection.serverConnection) {
    if (this.monicaConnection.serverConnection.monitoringServer ==
        server.host) {
      // we are already connected, simply return
      return;
    }
    // we are not connected to the right host, so disconnect
    this.disconnectServer();
  }

  // check whether we know about this host
  var knownHost = -1;
  for (var i = 0; i < this.availableServers.length; i++) {
    if (this.availableServers[i].host == server.host) {
      // yes, we know about this host already
      knownHost = i;
      break;
    }
  }

  // do we need to add this new host
  if (knownHost == -1) {
    // try to add it
    if (!server.name) {
      server.name = server.host;
    }
    this.availableServers.push(server);
    knownHost = this.availableServers.length - 1;
    // redraw
    this.draw();
  }

  // make the connection
  this.monicaConnection.serverConnection =
      new monicaConnection(this.availableServers[knownHost].host);
  this.monicaConnection.serverIndex = knownHost;

  // update the button states
  this.buttonStates();
};


/**
 * Disconnect from the server we're currently connected to.
 */
monicaHTMLFrontPage.prototype.disconnectServer = function() {

  // break the connection
  this.monicaConnection.serverConnection.disconnect();
  this.monicaConnection.serverConnection = null;
  this.monicaConnection.serverIndex = -1;

};


/**
 * Make the HTML for the MoniCA web client page.
 */
monicaHTMLFrontPage.prototype.draw = function() {

  // set up the front page
  // empty the header if necessary
  if (this.frontPageHeader != null) {
    dojo.empty(this.frontPageHeader);
  } else {
    // the top title header
    this.frontPageHeader = dojo.create('div', {'class': 'frontPageHeader'});
    dojo.byId(this.topDivId).appendChild(this.frontPageHeader);
  }

  this.frontPageHeader.appendChild(dojo.create('div',
      {innerHTML: 'MoniCA Web Client Page'}));

  // we'll also put our options and buttons up here, in some tables
  // the options table will float left
  var optionsDiv = dojo.create('div', {'class': 'frontPageOptionsDiv'});
  this.frontPageHeader.appendChild(optionsDiv);
  var optionsTable = dojo.create('table', {'class': 'frontPageOptionsTable'});
  optionsDiv.appendChild(optionsTable);

  var optionsTableRow = dojo.create('tr');
  optionsTable.appendChild(optionsTableRow);

  // the server to connect to
  optionsTableRow.appendChild(dojo.create('th', {innerHTML: 'MoniCA Server:'}));

  /**
   * A drop down box that we will use to display the names of the available
   * MoniCA servers.
   * @type {DOMNode}
   */
  this.monicaServersBox = dojo.create('select', {id: 'monicaServerSelection'});
  for (var i = 0; i < this.availableServers.length; i++) {
    this.monicaServersBox.appendChild(dojo.create('option',
        {value: this.availableServers[i].host,
          innerHTML: this.availableServers[i].name}));
  }
  var optionsTableCell = dojo.create('td');
  optionsTableCell.appendChild(this.monicaServersBox);
  optionsTableRow.appendChild(optionsTableCell);

  // the time between server updates
  optionsTableRow.appendChild(dojo.create('th',
      {innerHTML: 'Update Time (s):'}));

  /**
   * A text box input to allow the user to change the time between server
   * updates.
   * @type {DOMNode}
   */
  this.updateTimeBox = dojo.create('input',
      {type: 'text', id: 'updateTimeValue',
        value: this.updateTime, size: '3'});
  optionsTableCell = dojo.create('td');
  optionsTableCell.appendChild(this.updateTimeBox);
  optionsTableRow.appendChild(optionsTableCell);

  // the buttons to connect/disconnect from a server, and start/stop monitoring
  optionsTableRow = dojo.create('tr');
  optionsTable.appendChild(optionsTableRow);
  optionsTableCell = dojo.create('td', {colspan: '2'});
  optionsTableRow.appendChild(optionsTableCell);

  /**
   * A button that will connect or disconnect the user's selected MoniCA
   * server.
   * @type {button}
   */
  this.connectorButton = dojo.create('button', {type: 'button'});
  optionsTableCell.appendChild(this.connectorButton);

  optionsTableCell = dojo.create('td', {colspan: '2'});
  optionsTableRow.appendChild(optionsTableCell);

  /**
   * A button that will start or stop periodic updates from the selected
   * MoniCA server.
   * @type {button}
   */
  this.starterButton = dojo.create('button', {type: 'button'});
  optionsTableCell.appendChild(this.starterButton);
  // connect the buttons to their events
  dojo.connect(this.connectorButton, 'onclick', this, this.connectionHandler);
  dojo.connect(this.starterButton, 'onclick', this, this.updaterHandler);

  // the action buttons
  var buttonsDiv = dojo.create('div', {'class': 'frontPageButtonsDiv'});
  this.frontPageHeader.appendChild(buttonsDiv);
  var buttonsTable = dojo.create('table', {'class': 'frontPageButtonsTable'});
  buttonsDiv.appendChild(buttonsTable);

  var buttonsTableRow = dojo.create('tr');
  buttonsTable.appendChild(buttonsTableRow);

  // create a "new point table" button
  var buttonsTableCell = dojo.create('td');
  buttonsTableRow.appendChild(buttonsTableCell);

  /**
   * A button that the user can click to add a new point table to the web
   * client's front page.
   * @type {button}
   */
  this.newPointTableButton =
      dojo.create('button', {type: 'button', innerHTML: 'New Point Table'});
  buttonsTableCell.appendChild(this.newPointTableButton);
  buttonsTableCell = dojo.create('td');
  buttonsTableRow.appendChild(buttonsTableCell);

  /**
   * A button that the user can click to add a new time series plot to the
   * web client's front page.
   * @type {button}
   */
  this.newPlotButton =
      dojo.create('button', {type: 'button', innerHTML: 'New Time-Series'});
  buttonsTableCell.appendChild(this.newPlotButton);
  buttonsTableCell = dojo.create('td');
  buttonsTableRow.appendChild(buttonsTableCell);

  /**
   * A button that the user can click to load a preset container configuration
   * and put it on the web client's front page.
   * @type {button}
   */
  this.presetButton =
      dojo.create('button', {type: 'button', innerHTML: 'Load Preset'});
  buttonsTableCell.appendChild(this.presetButton);
  buttonsTableCell = dojo.create('td');
  buttonsTableRow.appendChild(buttonsTableCell);

  /**
   * A button that the user can click to generate a link that will recreate
   * the current front page configuration.
   * @type {button}
   */
  this.linkGenerateButton =
      dojo.create('button', {type: 'button', innerHTML: 'Generate Link'});
  buttonsTableCell.appendChild(this.linkGenerateButton);

  // connect the buttons to their events
  dojo.connect(this.newPointTableButton, 'onclick', this, this.addPointTable);
  dojo.connect(this.newPlotButton, 'onclick', this, this.addPlot);
  dojo.connect(this.linkGenerateButton, 'onclick', this, this.generateLink);

  // make our clearance div
  this.frontPageHeader.appendChild(dojo.create('div',
      {'class': 'frontPageClearDiv'}));

  // give the buttons their states
  this.buttonStates();

};


/**
 * Update the states of the buttons on the MoniCA web client front page.
 */
monicaHTMLFrontPage.prototype.buttonStates = function() {

  // the connection button
  if (this.monicaConnection.serverConnection == null) {
    this.connectorButton.innerHTML = 'Connect';
    // if we're not connected, we also can't do any actions
    this.newPointTableButton.disabled = true;
    this.newPlotButton.disabled = true;
    this.presetButton.disabled = true;
  } else {
    this.connectorButton.innerHTML = 'Disconnect ' +
        this.availableServers[this.monicaConnection.serverIndex].name;
    // since we're connected, we can allow new actions
    this.newPointTableButton.disabled = false;
    this.newPlotButton.disabled = false;
    this.presetButton.disabled = false;
  }

  // the updater button
  if (this.monicaConnection.serverConnection == null) {
    this.starterButton.innerHTML = 'Not connected';
    this.starterButton.disabled = true;
  } else if (this.monicaConnection.serverConnection.isUpdating == false) {
    this.starterButton.innerHTML = 'Start';
    this.starterButton.disabled = false;
  } else if (this.monicaConnection.serverConnection.isUpdating == true) {
    this.starterButton.innerHTML = 'Stop';
    this.starterButton.disabled = false;
  }

};


/**
 * Make or break a connection to a MoniCA server if the user has clicked
 * on our "Connect/Disconnect" button.
 */
monicaHTMLFrontPage.prototype.connectionHandler = function() {

  // check the current state
  if (this.connectorButton.innerHTML == 'Connect') {
    // make a connection
    // which MoniCA server is currently selected?
    var selectedHost = this.monicaServersBox.value;
    this.monicaConnection.serverConnection = new monicaConnection(selectedHost);
    // work out which one that is
    for (var i = 0; i < this.availableServers.length; i++) {
      if (this.availableServers[i].host == selectedHost) {
        this.monicaConnection.serverIndex = i;
        break;
      }
    }
  } else if (this.connectorButton.innerHTML.substr(0, 10) == 'Disconnect') {
    this.disconnectServer();
  }

  // update the button's state
  this.buttonStates();
};


/**
 * Start or stop periodic queries to the connected MoniCA server if the
 * user has clicked on our "Start/Stop" button.
 */
monicaHTMLFrontPage.prototype.updaterHandler = function() {

  // check the current state
  if (this.starterButton.innerHTML == 'Start') {
    // start monitoring
    this.monicaConnection.serverConnection.
        startMonitoring(this.updateTimeBox.value);
  } else if (this.starterButton.innerHTML == 'Stop') {
    // stop monitoring
    this.monicaConnection.serverConnection.stopMonitoring();
  }

  // update the button's state
  this.buttonStates();

};


/**
 * Add a MoniCA container and a point table to the MoniCA web client
 * page.
 */
monicaHTMLFrontPage.prototype.addPointTable = function() {

  // add a new point table
  // create its container
  this.childContainers.push(new monicaContainer({name: 'Container' +
        this.childContainers.length, parent: this}));
  // make the table itself
  this.childPointTables.
      push(new pointTable(this.monicaConnection.serverConnection,
      this.childContainers[this.childContainers.length - 1]));
  // add the container to the page
  dojo.byId(this.topDivId).
      appendChild(this.childContainers[this.childContainers.length - 1].
          domNode);

};


/**
 * Add a MoniCA container and a time series plot to the MoniCA web client
 * page.
 */
monicaHTMLFrontPage.prototype.addPlot = function() {

  // add a new plot
  // create its container
  this.childContainers.push(new monicaContainer({name: 'Container' +
        this.childContainers.length,
    parent: this}));
  // make the plot itself
  this.childPlots.push(new timeSeries(this.monicaConnection.serverConnection,
      this.childContainers[this.childContainers.length - 1]));
  // add the container to the page
  dojo.byId(this.topDivId).
      appendChild(this.childContainers[this.childContainers.length - 1].
          domNode);

};


/**
 * A container that we started has indicated that it will be stopping.
 * We need to remove it from our list of children.
 * @param {pointer} childPointer A pointer to the child container that is
 *                               closing.
 */
monicaHTMLFrontPage.prototype.childStopped = function(childPointer) {

  // a child is about to stop and is telling us
  // find any tables/plots it contains
  for (var i = 0; i < this.childPointTables.length; i++) {
    if (this.childPointTables[i] == childPointer.childObject) {
      // remove this table
      this.childPointTables.splice(i, 1);
      break;
    }
  }

  // find the container link now
  for (var i = 0; i < this.childContainers.length; i++) {
    if (this.childContainers[i] == childPointer) {
      // remove this child now
      this.childContainers.splice(i, 1);
    }
  }

};


/**
 * Generate link text that when put into the browser's address bar will
 * recreate this configuration exactly.
 */
monicaHTMLFrontPage.prototype.generateLink = function() {

  // generate a link that will regenerate the current page configuration
  // the current address
  var currentBaseHREFElements = location.href.split(/\?/);
  var baseHREF = currentBaseHREFElements[0];

  var optionsString = '?';

  // the server and update time
  if (this.monicaConnection.serverConnection == null) {
    // can't replicate a page that doesn't have a connection
    return;
  }
  optionsString += 'server=' +
      this.monicaConnection.serverConnection.monitoringServer;
  optionsString += '&updateTime=' +
      this.monicaConnection.serverConnection.updateTime;

  // go through each of our children
  for (var i = 0; i < this.childContainers.length; i++) {
    //    for (var i=0;i<this.childPointTables.length;i++){
    if (this.childContainers[i].childType == 'pointTable') {
      optionsString += '&pointTable=';
      for (var j = 0;
          j < this.childContainers[i].childObject.hasPoints.length; j++) {
        if (j != 0) {
          optionsString += ',';
        }
        optionsString += this.childContainers[i].childObject.hasPoints[j];
      }

    } else if (this.childContainers[i].childType == 'timeSeries') {
      //    for (var i=0;i<this.childPlots.length;i++){
      optionsString += '&timeSeries=';
      // the point names
      for (var j = 0;
          j < this.childContainers[i].childObject.hasPoints.length; j++) {
        if (j != 0) {
          optionsString += ',';
        }
        optionsString += this.childContainers[i].childObject.hasPoints[j];
      }
      // the time span
      optionsString += ',' +
          this.childContainers[i].childObject.timeIntervalInput.value;
      // the start time
      if ((this.childContainers[i].childObject.timeNowInput.checked == true) ||
          (this.childContainers[i].childObject.timeStartBox.value == '')) {
        optionsString += ',-1';
      } else {
        optionsString += ',' +
            this.childContainers[i].childObject.timeStartBox.value;
      }
      // the number of points returned
      if ((this.childContainers[i].childObject.maxPointsCheckbox.checked ==
          false) ||
          (this.childContainers[i].childObject.maxPointsInput.value == '')) {
        optionsString += ',-1';
      } else {
        optionsString += ',' +
            this.childContainers[i].childObject.maxPointsInput.value;
      }
    }
  }

  // copy the link into the popup dialog text box
  this.dialogLinkBox.value = baseHREF + optionsString;
  // make that row of the table show up
  this.dialogLinkRow.style.visibility = 'visible';
  // show the dialog
  this.dialogPopup.show();

};



/**
 * Make a link parsing object to take the URL currently in the browser's
 * address bar, and create either a MoniCA web client page or a series
 * of containers on a blank page to recreate the containers specified in
 * the URL.
 * @param {object} options Options for constructing the page from the link.
 * @constructor
 */
function linkParser(options) {

  // options contains the way to set up
  if (!options) {
    document.write('Unable to setup page according to link: no information!');
    return;
  }

  // get the setup information, if any
  var setupInfo = location.search;
  if (setupInfo.length == 0) {
    return;
  }
  setupInfo += '&';

  // find the server name
  var response = this.tagValue({tag: 'server', string: setupInfo});
  var serverName = response.value;
  setupInfo = response.remainder;
  if (options.frontPage) {
    // use the page connector function
    options.frontPage.connectServer({host: serverName});
  } else {
    // set up our own connection
    /**
     * A MoniCA server connection required by the link URL.
     * @type {monicaConnection}
     */
    this.monicaConnection = new monicaConnection(serverName);
  }

  // the update time
  response = this.tagValue({tag: 'updateTime', string: setupInfo});
  var updateTime = response.value;
  setupInfo = response.remainder;

  // what is the next available token?
  var nextToken = this.nextToken(setupInfo);
  while (nextToken != null) {
    response = this.tagValue({tag: nextToken, string: setupInfo});
    var tokenPoints = response.value;
    setupInfo = response.remainder;
    if (nextToken == 'pointTable') {
      // setup a point table
      // split up the points
      var allPoints = tokenPoints.split(/\,/);
      // check for null options
      for (var i = 0; i < allPoints.length; i++) {
        if (allPoints[i] == '') {
          allPoints.splice(i, 1);
          i--;
        }
      }
      if (options.frontPage) {
        // we setup on a MoniCA HTML front page
        options.frontPage.addPointTable();
        // add the points to that table
        var pointTablePointer =
            options.frontPage.childPointTables[options.frontPage.
            childPointTables.length - 1];
        pointTablePointer.addPoints(allPoints);

      } else if (options.topDivId) {
        // we have a blank page to use
        // make a container
        var pointTableContainer = new monicaContainer('pointTable');
        // make the point table
        var pointTableTable =
            new pointTable(this.monicaConnection, pointTableContainer);
        // attach it to the page
        dojo.byId(topDivId).appendChild(pointTableContainer.domNode);
        // add the points
        pointTableTable.addPoints(allPoints);
      }
    } else if (nextToken == 'timeSeries') {
      // setup a time series
      // split up the points
      var allPoints = tokenPoints.split(/\,/);
      // check for null options
      for (var i = 0; i < allPoints.length; i++) {
        if (allPoints[i] == '') {
          allPoints.splice(i, 1);
          i--;
        }
      }
      if (options.frontPage) {
        // we setup on a MoniCA HTML front page
        options.frontPage.addPlot();
        // add the points to that time series
        var newMaxPoints = allPoints.pop();
        var newStartTime = allPoints.pop();
        var newTimeInterval = allPoints.pop();
        var timeSeriesPointer =
            options.frontPage.childPlots[options.frontPage.childPlots.
            length - 1];
        timeSeriesPointer.addPoints(allPoints);
        timeSeriesPointer.setPlotTime(newStartTime, newTimeInterval,
            newMaxPoints);
        timeSeriesPointer.updatePlot();
      } else if (options.topDivId) {
        // we have a blank page to use
        // make a container
        var timeSeriesContainer = new monicaContainer('timeSeries');
        // make the time series
        var timeSeriesPlot = new pointTable(this.monicaConnection,
            timeSeriesContainer);
        // attach it to the page
        dojo.byId(topDivId).appendChild(timeSeriesContainer.domNode);
        // add the points
        var newMaxPoints = allPoints.pop();
        var newStartTime = allPoints.pop();
        var newTimeInterval = allPoints.pop();
        timeSeriesPlot.addPoints(allPoints);
        timeSeriesPlot.setPlotTime(newStartTime, newTimeInterval, newMaxPoints);
        timeSeriesPlot.updatePlot();
      }
    }

    nextToken = this.nextToken(setupInfo);
  }

  // start the page updating if required
  if (updateTime > 0) {
    if (options.frontPage) {
      options.frontPage.updateTimeBox.value = updateTime;
      options.frontPage.updaterHandler();
    } else if (options.topDivId) {
      this.monicaConnection.startMonitoring(updateTime);
    }
  }
}


/**
 * Find the next token in the URL.
 * @param {string} string The part of the URL that has not yet been parsed.
 * @return {string} The next token.
 */
linkParser.prototype.nextToken = function(string) {

  // find the string directly after the next question mark (?)
  var elements = /^\?*(\S+?)\=.*$/.exec(string);
  if (!elements) {
    return (null);
  }
  return (elements[1]);

};


/**
 * Find the value of the requested tag.
 * @param {object} args An object describing the token to find the value of.
 * @return {object} An object with the value and the URL portion that has yet
 *                   to be parsed.
 */
linkParser.prototype.tagValue = function(args) {

  if ((!args) || (!args.tag) || (!args.string)) {
    return (null);
  }

  var elementsExp = new RegExp('^(.*?)' + args.tag + '\\=(\\S+?)\\&(.*)$', '');
  var elements = elementsExp.exec(args.string);
  var value = elements[2];
  var remainder = elements[1] + elements[3];

  return ({ value: value, remainder: remainder });
};



/**
 * Make a time series plot in a MoniCA container.
 * @param {pointer} monicaServer A pointer to the monicaServer object that
 *                               will be used to update our plots.
 * @param {pointer} monicaContainer A pointer to the monicaContainer that
 *                                  our top node will attach to.
 * @constructor
 */
function timeSeries(monicaServer, monicaContainer) {

  /**
   * A list of points that we will display in our time series plot.
   * @type {string}
   */
  this.hasPoints = [];

  /**
   * The time span of the plot, in minutes.
   * @type {int}
   */
  this.timeSpan = 60; // default 1 hour

  /**
   * The "starting time" of the plot, which can either be the number -1,
   * which indicates the plot should show the latest timeSpan minutes of
   * data, or a time formatted as 'yyyy-mm-dd HH:MM:SS'.
   * @type {various}
   */
  this.startTime = -1; // default is no start time (ie. last timeSpan)

  /**
   * The DOM node used to hold the time series plot.
   * @type {DOMNode}
   */
  this.contentDomNode = dojo.create('div', {'class': 'timeSeriesDiv'});

  /**
   * The time series plot object, which will be null until it is created
   * by our method updatePlot.
   * @type {Highchart}
   */
  this.plotObject = null;

  /**
   * The DOM node used to hold all the editing functions.
   * @type {DOMNode}
   */
  this.editDomNode = dojo.create('div', {'class': 'timeSeriesEdit'});

  /**
   * The monicaConnection object that we will use to get the values for
   * our points.
   * @type {pointer}
   */
  this.monicaServer = monicaServer;

  /**
   * The monicaContainer that we are attached to, and that will handle our
   * content and editing panes.
   * @type {pointer}
   */
  this.monicaContainer = monicaContainer;

  // set all the required values in the server and container
  this.monicaContainer.childObject = this; // we are the child of the container
  this.monicaContainer.childType = 'timeSeries';

  /**
   * A pointer to that our update method, that is required so the container
   * knows how to get us to update when required.
   * @type {pointer}
   */
  this.updaterFunction = this.updatePlot;

  /**
   * A flag to indicate that we need to be updated, usually because the user
   * has added or removed some points, or has changed the time range to
   * display.
   * @type {boolean}
   */
  this.hasChanged = true;

  /**
   * A flag to indicate whether the user has added or removed a point from
   * the plot.
   * @type {boolean}
   */
  this.pointsChanged = false; // only if a point has been added/removed

  // append our nodes to the container
  this.monicaContainer.content.appendChild(this.contentDomNode);
  this.monicaContainer.editor.appendChild(this.editDomNode);

  // add the editing functions
  /**
   * The DOM node that will form the top line of the editing pane, showing
   * the time range inputs.
   * @type {DOMNode}
   */
  this.timeSelectionDomNode = dojo.create('div',
      {'class': 'plotTimeSelectDiv'});
  this.editDomNode.appendChild(this.timeSelectionDomNode);

  /**
   * A table to format the time range inputs nicely.
   * @type {DOMNode}
   */
  this.timeSelectionTable = dojo.create('table');
  this.timeSelectionDomNode.appendChild(this.timeSelectionTable);

  var timeRow = dojo.create('tr');
  this.timeSelectionTable.appendChild(timeRow);
  var timeCell = dojo.create('th', {innerHTML: 'Plot: '});
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td');
  timeRow.appendChild(timeCell);

  /**
   * The text input to specify the interval to show on the plot.
   * @type {input}
   */
  this.timeIntervalInput = dojo.create('input', {type: 'text', size: '5',
    value: '60'});
  timeCell.appendChild(this.timeIntervalInput);
  timeCell = dojo.create('td', {innerHTML: 'minutes'});
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('th', {innerHTML: 'starting: '});
  timeRow.appendChild(timeCell);

  /**
   * A radio button that will indicate that the user wants the most recent
   * data shown on the plot.
   * @type {radioButton}
   */
  this.timeNowInput = dojo.create('input', {type: 'radio', name: 'timeStarting',
    value: 'now', checked: 'true'});
  timeCell = dojo.create('td');
  timeCell.appendChild(this.timeNowInput);
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td', {innerHTML: 'now'});
  timeRow.appendChild(timeCell);

  /**
   * A radio button that will indicate that the user wants to plot data
   * starting at a specific time.
   * @type {radioButton}
   */
  this.timeStartInput = dojo.create('input',
      {type: 'radio', name: 'timeStarting', value: 'then'});
  timeCell = dojo.create('td');
  timeCell.appendChild(this.timeStartInput);
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td', {innerHTML: 'time:'});
  timeRow.appendChild(timeCell);

  /**
   * A text input to allow the user to specify the earliest time to display
   * data for.
   * @type {input}
   */
  this.timeStartBox = dojo.create('input', {name: 'startTime', id: 'startTime',
    type: 'text', size: '20'});
  timeCell = dojo.create('td');
  timeCell.appendChild(this.timeStartBox);
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td');
  timeRow.appendChild(timeCell);

  /**
   * A date/time picker to allow the user to easily fill the starting time
   * input box.
   * @type {dateTimePicker}
   */
  this.timeStartHREF =
      "javascript:NewCssCal('startTime','yyyymmdd','dropdown',true,24,false)";

  /**
   * An image of a calendar that when clicked will bring up the date/time
   * picker.
   * @type {image}
   */
  this.timeStartBoxSelector = dojo.create('a', {href: this.timeStartHREF,
    innerHTML: '<img src="images/cal.gif" width="16" height="16"' +
        ' alt="Pick a date">'});
  timeCell.appendChild(this.timeStartBoxSelector);
  timeCell = dojo.create('td');
  timeRow.appendChild(timeCell);

  /**
   * A checkbox indicating that the number of points that the MoniCA server
   * returns per query should be limited to a manageable number. A limit
   * should be placed on the number of points returned in almost all
   * situations, so unselecting this box is not recommended.
   * @type {checkbox}
   */
  this.maxPointsCheckbox = dojo.create('input',
      {type: 'checkbox', name: 'maxPointsCheck',
        value: 'true', checked: true});
  timeCell.appendChild(this.maxPointsCheckbox);
  timeCell = dojo.create('td', {innerHTML: 'max # points: '});
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td');
  timeRow.appendChild(timeCell);

  /**
   * A text input that allows the user to specify the maximum number of
   * points that the MoniCA server should return per query.
   * @type {input}
   */
  this.maxPointsInput = dojo.create('input', {type: 'text', name: 'maxPoints',
    size: '5', value: '200'});
  timeCell.appendChild(this.maxPointsInput);
  // connect the radio buttons to the button state updater

  /**
   * An event handler that is triggered when the user clicks the 'now' radio
   * button.
   * @type {eventHandle}
   */
  this.timeHandle1 =
      dojo.connect(this.timeNowInput, 'onclick', this, this.buttonStates);

  /**
   * An event handler that is triggered when the user clicks the 'time' radio
   * button.
   * @type {eventHandle}
   */
  this.timeHandle2 =
      dojo.connect(this.timeStartInput, 'onclick', this, this.buttonStates);

  /**
   * An event handler that is triggered when the user enables/disables the
   * checkbox associated with the maximum number of returned points.
   * @type {eventHandle}
   */
  this.maxPointsHandle =
      dojo.connect(this.maxPointsCheckbox, 'onclick', this, this.buttonStates);

  /**
   * An event handler that is triggered when the user changes the maximum
   * number of returned points.
   * @type {eventHandle}
   */
  this.maxPointsChangeHandle =
      dojo.connect(this.maxPointsInput, 'onchange', this, this.valuesUpdated);

  /**
   * An event handler that is triggered when the user changes the time
   * interval.
   * @type {eventHandle}
   */
  this.intervalChangeHandle =
      dojo.connect(this.timeIntervalInput, 'onchange', this,
          this.valuesUpdated);

  /**
   * An event handler that is triggered when the user changes the starting
   * time.
   * @type {eventHandle}
   */
  this.timeChangeHandle =
      dojo.connect(this.timeStartBox, 'onchange', this, this.valuesUpdated);

  // the tree of all available monitoring points, taken from the parent
  // MoniCA server
  /**
   * The DOM node that contains our tree menu of all available MoniCA points.
   * @type {DOMNode}
   */
  this.treeSideDiv = dojo.create('div', {'class': 'plotTreeSideDiv'});
  this.treeSideDiv.appendChild(dojo.create('p',
      {innerHTML: 'Available Points'}));

  /**
   * The parent DOM node of our tree menu.
   * @type {DOMNode}
   */
  this.treeDiv = dojo.create('div', {'class': 'plotTreeDiv'});

  /**
   * A Dojo tree menu that is populated with the model of all available MoniCA
   * point names, obtained from our attached MoniCA server.
   */
  this.treeControl = new dijit.Tree({
    model: this.monicaServer.treeModel,
    showRoot: false,
    'class': 'plotTreeControl'});
  this.treeDiv.appendChild(this.treeControl.domNode);
  this.treeSideDiv.appendChild(this.treeDiv);
  this.editDomNode.appendChild(this.treeSideDiv);

  /**
   * An event handle that will be triggered whenever the user double-clicks
   * one of the MoniCA points in the tree menu.
   * @type {eventHandle}
   */
  this.treeHandle =
      dojo.connect(this.treeControl, 'onDblClick', this, this.addFromClick);

  /**
   * The DOM node that will contain a multi-select box that will show all
   * the MoniCA point names that are currently being displayed in the point
   * table.
   * @type {DOMNode}
   */
  this.editSelectDiv = dojo.create('div', {'class': 'plotEditDiv'});
  this.editSelectDiv.appendChild(dojo.create('p',
      {innerHTML: 'Points in plots'}));

  /**
   * The parent DOM node of our multi-select box.
   * @type {DOMNode}
   */
  this.editSelectScrollDiv = dojo.create('div', {'class': 'plotEditScrollDiv'});
  this.editSelectDiv.appendChild(this.editSelectScrollDiv);

  /**
   * The multi-select box that will show all the MoniCA point names that
   * are currently part of the point table.
   * @type {DOMNode}
   */
  this.editSelect = dojo.create('select', {multiple: 'multiple', size: '20',
    'class': 'plotEditSelect'});
  this.editSelectScrollDiv.appendChild(this.editSelect);

  /**
   * A button that will allow the user to remove all the points that they have
   * selected in the multi-select box from the point table.
   * @type {button}
   */
  this.editRemoveButton =
      dojo.create('button', {type: 'button', innerHTML: 'Remove Points',
        'class': 'plotEditRemoveButton'});
  this.editSelectDiv.appendChild(this.editRemoveButton);

  /**
   * An event handler that is triggered when the user hits the "Remove Points"
   * button.
   * @type {eventHandle}
   */
  this.removeButtonHandle =
      dojo.connect(this.editRemoveButton, 'onclick', this, this.removePoints);

  /**
   * An event handler that is triggered when the user double-clicks a point
   * in the multi-select box, to remove the point from the point table.
   * @type {eventHandle}
   */
  this.removeClickHandle =
      dojo.connect(this.editSelect, 'ondblclick', this, this.removeFromClick);
  this.editDomNode.appendChild(this.editSelectDiv);

  /**
   * A list of all the MoniCA point names that are being displayed in the
   * point table, formatted to be inserted into the multi-select input
   * box.
   * @type {DOMNodeArray}
   */
  this.editSelectOptions = []; // we don't yet have any points

  // start the buttons in the right state
  this.buttonStates();
}


/**
 * Set the time range of the plot programmatically, and apply it to all the
 * points that we are plotting.
 * @param {time} startTime The starting time of the plot; -1 means "up till
 *                         now", otherwise the time should be of the format
 *                         "yyyy-mm-dd HH:MM:SS".
 * @param {number} timeInterval The time range to show on the plot, as a
 *                              number in minutes.
 * @param {int} maxPoints The maximum number of points to request from the
 *                        server per MoniCA request.
 */
timeSeries.prototype.setPlotTime = function(startTime, timeInterval,
    maxPoints) {
  if (startTime != null) {
    if (startTime == -1) {
      // "now"
      this.timeNowInput.checked = true;
      this.timeStartInput.checked = false;
    } else {
      this.timeNowInput.checked = false;
      this.timeStartInput.checked = true;
      this.timeStartBox.value = startTime;
    }
    this.hasChanged = true;
  }

  if (timeInterval != null) {
    this.timeIntervalInput.value = timeInterval;
    this.hasChanged = true;
  }

  if (maxPoints != null) {
    if (maxPoints == -1) {
      // no limit on points
      this.maxPointsCheckbox.checked = false;
    } else {
      this.maxPointsCheckbox.checked = true;
      this.maxPointsInput.value = maxPoints;
    }
  }

  this.buttonStates();
};


/**
 * Destroy this time series and free its memory.
 */
timeSeries.prototype.destroy = function() {
  // we need to provide this function for the container to close us correctly

  // get rid of all our points from the MoniCA server
  this.monicaServer.removeTimeSeriesPoint(null, this);

  // destroy the plot
  if (this.plotObject) {
    this.plotObject.destroy();
  }

  // disconnect our buttons
  dojo.disconnect(this.timeHandle1);
  dojo.disconnect(this.timeHandle2);
  dojo.disconnect(this.treeHandle);
  dojo.disconnect(this.removeButtonHandle);
  dojo.disconnect(this.removeClickHandle);
  dojo.disconnect(this.maxPointsHandle);
  dojo.disconnect(this.maxPointsChangeHandle);
  dojo.disconnect(this.intervalChangeHandle);
  dojo.disconnect(this.timeChangeHandle);
};


/**
 * Check and update the states of the time select row buttons, and input
 * boxes.
 */
timeSeries.prototype.buttonStates = function() {

  // check which radio selection button is selected
  if (this.timeNowInput.checked) {
    // disable all the specific time selection stuff
    this.timeStartBox.disabled = true;
    this.timeStartBoxSelector.href = 'javascript:return false;';
  } else if (this.timeStartInput.checked) {
    // enable all the specific time selection stuff
    this.timeStartBox.disabled = false;
    this.timeStartBoxSelector.href = this.timeStartHREF;
  }

  if (this.maxPointsCheckbox.checked) {
    // enable the number of points box
    this.maxPointsInput.disabled = false;
  } else {
    this.maxPointsInput.disabled = true;
  }
  this.hasChanged = true;
};


/**
 * Add a point to our plot after the user has double-clicked the point in
 * our tree menu on the edit pane.
 * @param {object} evtObj A pointer to the tree menu.
 */
timeSeries.prototype.addFromClick = function(evtObj) {
  // determine the item that was clicked
  var clickedId = this.monicaServer.store.getValue(evtObj, 'id');
  var clickedElements = clickedId.split(/\./);
  // add this element to the points
  var clickedIds = [clickedId];
  this.addPoints(clickedIds);

};


/**
 * Add one or more points to our plot programmatically.
 * @param {array} points An array of strings, where each element is the name
 *                       a MoniCA point to add to our plot.
 */
timeSeries.prototype.addPoints = function(points) {

  // all the new points will be stored here
  var allNew = [];

  // cycle through the points we've been asked to add
  for (var i = 0; i < points.length; i++) {
    // check that the point isn't already there
    var pointExists = 0;
    for (var j = 0; j < this.hasPoints.length; j++) {
      if (points[i] == this.hasPoints[j]) {
        pointExists = 1;
        break;
      }
    }
    if (pointExists == 0) {
      // add the point to our list
      this.hasPoints.push(points[i]);
      allNew.push(points[i]);
      // add a new option to the select
      this.editSelectOptions.push(dojo.create('option',
          {innerHTML: points[i]}));
      this.editSelect.appendChild(this.
          editSelectOptions[this.editSelectOptions.length - 1]);
    }
  }

  // add the points to the update list
  this.monicaServer.addTimeSeriesPoint(allNew, this);

  this.hasChanged = true;
  this.pointsChanged = true;
};


/**
 * Remove a point from our plot after the user has double-clicked it in the
 * select box showing the current points on the plot in the edit pane.
 * @param {object} eventObj A pointer to the select box in the edit pane.
 */
timeSeries.prototype.removeFromClick = function(eventObj) {
  // determine the item that was clicked
  var clickedSelectOption = eventObj.target;

  // remove from the MoniCA list
  this.monicaServer.removeTimeSeriesPoint([clickedSelectOption.innerHTML],
      this);

  for (var i = 0; i < this.hasPoints.length; i++) {
    if (this.hasPoints[i] == clickedSelectOption.innerHTML) {
      this.hasPoints.splice(i, 1);
      break;
    }
  }
  dojo.destroy(clickedSelectOption);

  // indicate we need an update
  this.hasChanged = true;
  this.pointsChanged = true;
};


/**
 * Remove points from our plot after the user has selected them in the select
 * box on the edit pane and clicked the "Remove Points" button.
 */
timeSeries.prototype.removePoints = function() {

  // find all the currently selection points from the multi-select
  var allToRemove = [];

  for (var i = 0; i < this.editSelectOptions.length; i++) {
    if (this.editSelectOptions[i].selected) {
      allToRemove.push(this.editSelectOptions[i].innerHTML);
      // find this point
      for (var j = 0; j < this.hasPoints.length; j++) {
        if (this.hasPoints[j] == this.editSelectOptions[i].innerHTML) {
          this.hasPoints.splice(j, 1);
          break;
        }
      }
      dojo.destroy(this.editSelectOptions[i]);
      this.editSelectOptions.splice(i, 1);
      i--;
    }
  }

  // remove from the MoniCA list
  this.monicaServer.removeTimeSeriesPoint(allToRemove, this);

  // indicate we need an update
  this.hasChanged = true;
  this.pointsChanged = true;
};


/**
 * Indicate that the server parameters need to be updated after the user
 * has updated the starting time, the time interval, or the maximum number
 * of returned points.
 * @param {object} evtObj A pointer ot the field that the user has updated.
 */
timeSeries.prototype.valuesUpdated = function(evtObj) {
  this.hasChanged = true;
};


/**
 * Handle the creation of the plot surface, updating the server parameters
 * to get the data for the plot, and the addition/removal/maintenance of the
 * time series on the plot.
 */
timeSeries.prototype.updatePlot = function() {

  // update the MoniCA server with our plot time range
  var timeStart = -1;
  if (this.timeStartInput.checked) {
    timeStart = this.timeStartBox.value.replace(/\s+/g, ':');
  }
  var maxNPoints = -1;
  if (this.maxPointsInput.disabled == false) {
    maxNPoints = this.maxPointsInput.value;
  }
  var rangeChanged =
      this.monicaServer.setTimeSeriesPointRange(this, timeStart,
      this.timeIntervalInput.value, maxNPoints);

  // make a plot area if one doesn't yet exist
  if (!this.plotObject) {
    Highcharts.setOptions({ global: { useUTC: true },
          tooltip: { formatter: function() {
            var seriesName;
            var seriesUnits;
            var nameElements = /^(.*)\s\[(.*)\]$/.exec(this.series.name);
            if (nameElements == null) {
              // didn't match the style, so probably no units
              seriesName = this.series.name;
              seriesUnits = '';
            } else {
              seriesName = nameElements[1];
              seriesUnits = nameElements[2];
            }
            var s = ['<span style="font-size: 10px">',
              Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x),
              '</span><br/>',
              '<span style="color:' + this.series.color + '">',
              seriesName, '</span> = ', this.y,
              ' ' + seriesUnits];
            return s.join('');
          }}});
    this.plotObject = new Highcharts.Chart({
      chart: { renderTo: this.contentDomNode, defaultSeriesType: 'line' },
      title: { text: 'MoniCA plot' },
      xAxis: { type: 'datetime', title: { enabled: true, text: 'Time (UT)',
        startOnTick: false, endOnTick: false,
        showLastLabel: true }},
      yAxis: { title: { text: 'Value' }},
      series: []
    });
  }

  // if we have changed we need to get the descriptions for our points
  this.hasChanged = false;
  if (this.pointsChanged == true) {
    this.pointsChanged = false;
    this.monicaServer.getDescriptions();
    return;
  } else if (rangeChanged == true) {
    this.monicaServer.pollValues();
    return;
  }

  // set the loading state
  this.plotObject.showLoading('Please wait, getting points from MoniCA server');

  // request the data
  var timeSeriesData = this.monicaServer.getTimeSeriesPoints(this);

  // go and add the data
  for (var i = 0; i < timeSeriesData.length; i++) {
    timeSeriesData[i].name = this.modifyName(timeSeriesData[i].name);
    // search for this series
    var seriesIndex = -1;
    for (var j = 0; j < this.plotObject.series.length; j++) {
      if (this.plotObject.series[j].name == timeSeriesData[i].name) {
        seriesIndex = j;
        break;
      }
    }
    if (seriesIndex == -1) {
      // add a new series
      this.plotObject.addSeries(timeSeriesData[i], false);
    } else {
      this.plotObject.series[seriesIndex].setData(timeSeriesData[i].data,
          false);
    }
  }

  // remove any series that have been removed since last update
  for (var i = 0; i < this.plotObject.series.length; i++) {
    var isPresent = -1;
    for (var j = 0; j < this.hasPoints.length; j++) {
      if (this.plotObject.series[i].name ==
          this.modifyName(this.hasPoints[j])) {
        isPresent = j;
        break;
      }
    }
    if (isPresent == -1) {
      this.plotObject.series[i].remove(false);
      i--;
    }
  }
  this.plotObject.redraw();

  this.plotObject.hideLoading();
};


/**
 * Modify the name of the series to be more descriptive when shown at the
 * bottom of the plot.
 * @param {string} name The name of the series, which should be the name of
 *                      a MoniCA point.
 * @return {string} The modified, descriptive name.
 */
timeSeries.prototype.modifyName = function(name) {
  // get the units associated with this point
  var pointUnits = this.monicaServer.monPoints[name].units;

  var nameElements = name.split(/\./g);
  var newName = nameElements[0] + ' ' + nameElements[nameElements.length - 1];
  if (pointUnits != '') {
    newName += ' [' + pointUnits + ']';
  }

  return newName;
};

