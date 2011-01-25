dojo.require('dijit.Dialog');
dojo.require('dojo.parser');
dojo.require('dojo.NodeList-traverse');
dojo.require('dojo.data.ItemFileReadStore');
dojo.require('dijit.Tree');
dojo.require('dojox.timing._base');

// our MoniCA connection object
function monicaConnection(host) {
  // the host we're connecting to
  if (!host) {
    this.monitoringServer = 'monhost-nar';
  } else {
    this.monitoringServer = host;
  }
  // the points this object is responsible for polling for point tables
  this.requestedPoints = [];
  this.pointRequests = []; // the number of times each point is requested
  // the data returned from a poll
  this.returnedData = [];

  // the points this object is responsible for polling for time series plots
  // the point name
  this.intervalPoints = [];
  // the object that requests each point
  this.intervalPointRequester = [];
  // the start time for the time series
  this.intervalStartTimes = [];
  // the length of time for the time series
  this.intervalTimeInterval = [];
  // the maximum number of points returned per query
  this.intervalMaxPoints = [];
  // we don't need to continually update static time series
  this.intervalCompleted = [];
  // the response from the server for this point
  this.intervalPointResponse = [];

  // all the points available from this server
  this.availableMonitoringPoints = [];
  // metadata about the monitoring points
  this.monPoints = {};

  // the update interval object
  this.updateTime = -1;
  this.updateInterval = new dojox.timing.Timer();
  this.isUpdating = false;

  // initialise the connection
  this.initialise();
}

monicaConnection.prototype.disconnect = function() {
  for (var i in this) {
    delete this[i];
  }
};

// this handles communications with the Perl MoniCA client on the server
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

// this initialisation function will obtain a list of all available
// monitoring points from the MoniCA server
monicaConnection.prototype.initialise = function() {

  this.comms('names');

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

  this.requestedPoints = [];

  // prepare the dojo tree model for all the children
  this.store = new dojo.data.ItemFileReadStore({
    data: { identifier: 'id', label: 'label', items: this.oMPTree }});
  this.treeModel = new dijit.tree.ForestStoreModel({ store: this.store });

};

// get the metadata for each of the points that we are responsible
// for polling
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

monicaConnection.prototype.pollValues = function() {

  // get the current values of all the requested points
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
	wantPoints += this.intervalPoints[i] + ',' + this.intervalStartTimes[i] +
	  ',' + this.intervalTimeInterval[i] + ',' + this.intervalMaxPoints[i];
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
	this.intervalPointResponse[i] = this.convertToNumbers(this.intervalPointResponse[i]);
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

function isNumber(n) {
  return typeof n === 'number' && isFinite(n);
}

monicaConnection.prototype.convertToNumbers = function(JSONdata) {

  // takes some JSON data we may expect, and converts it to numerical value
  // eg. when the returned values are DMS
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

monicaConnection.prototype.updateClass = function(targetClass,newValue,pointOK,classDescription,classUnits) {

  var searchClassName = this.safifyClassName(targetClass);
  var descriptionClassName = 'description_' + searchClassName;
  var unitsClassName = 'units_' + searchClassName;

  // clean up the value
  // removes silly symbol coming with some phases
  newValue = newValue.replace(/[\u00c2><]/g, '');

  dojo.query('.' + searchClassName).forEach(function(node,index,arr) {
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
  dojo.query('.' + descriptionClassName).forEach(function(node,index,arr) {
    if (dojo.isIE) {
	node.innerText = classDescription;
    } else {
	node.innerHTML = classDescription;
    }
  });
  dojo.query('.' + unitsClassName).forEach(function(node,index,arr) {
    if (dojo.isIE) {
	node.innerText = classUnits;
    } else {
	node.innerHTML = classUnits;
    }
  });
};

monicaConnection.prototype.safifyClassName = function(inputClass) {
  var stage1 = inputClass.replace(/\./g, '');
  var stage2 = stage1.replace(/\+/g, 'plus');
  return (stage2);
};

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

monicaConnection.prototype.addTimeSeriesPoint = function(points,requestObject) {

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

monicaConnection.prototype.removeTimeSeriesPoint = function(points,requestObject) {

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

monicaConnection.prototype.setTimeSeriesPointRange = function(requestObject,startTime,timeInterval,maxPoints) {

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

monicaConnection.prototype.stopMonitoring = function() {
  // stop any current intervals
  this.updateInterval.stop();
  this.isUpdating = false;
};

// our movable MoniCA container
function monicaContainer(options) {
  // were we passed options?
  this.containerName = '';
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
  this.domNode = dojo.create('div');
  dojo.attr(this.domNode, 'class', 'monicaContainer');
  if (this.containerName != '') {
    this.domNode.id = this.containerName;
  }

  // make this div moveable to anywhere on the page
  //    this.dnd=new dojo.dnd.Moveable(this.domNode);

  // make the button to show either the content or the edit boxes
  this.changeViewButton = dojo.create('button', {type: 'button', 'class': 'changeViewButton',
						 innerHTML: 'Edit'});
  this.domNode.appendChild(this.changeViewButton);
  // make the edit button do what it is supposed to do
  this.viewButtonHandle = dojo.connect(this.changeViewButton, 'onclick', this, this.switchView);
  // add the container close button
  this.closeViewImage = dojo.create('img', {src: 'closebutton.png',
					    'class': 'closeButton',
					    width: '16px', height: '16px'});
  this.domNode.appendChild(this.closeViewImage);
  this.closeButtonHandle = dojo.connect(this.closeViewImage, 'onclick', this, this.destroyContainer);

  // make two overlapping containers, one for the content, the other
  // for the editing functions
  this.overlapTable = dojo.create('table', {'class': 'containerOverlapTable'});
  this.contentRow = dojo.create('tr');
  this.overlapTable.appendChild(this.contentRow);
  this.editorRow = dojo.create('tr');
  this.overlapTable.appendChild(this.editorRow);
  this.content = dojo.create('div', {'class': 'containerContent'});
  this.editor = dojo.create('div', {'class': 'containerEditor'});
  this.contentRow.appendChild(this.content);
  this.editorRow.appendChild(this.editor);
  this.domNode.appendChild(this.overlapTable);

  // set the initial visibilities
  this.contentRow.style.visibility = 'visible';
  this.editorRow.style.visibility = 'collapse';
  this.isShowing = true;

  // a pointer to the object that we will eventually wrap around
  this.childObject = null;
  this.childType = '';
}

monicaContainer.prototype.destroyContainer = function(evtObj) {
  // we have to nicely close this container and its child

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

// our point table object
function pointTable(monicaServer,monicaContainer) {
  // the points we are displaying
  this.hasPoints = [];

  // the div to contain the rendered points table
  this.contentDomNode = dojo.create('div', {'class': 'pointTableDiv'});
  // the div to contain the editing functions
  this.editDomNode = dojo.create('div', {'class': 'pointTableEdit'});

  // the MoniCA server that will update us, and the MoniCA container that holds us
  this.monicaServer = monicaServer;
  this.monicaContainer = monicaContainer;
  // set all the required values in the server and container
  this.monicaContainer.childObject = this; // we are the child object of the container
  this.monicaContainer.childType = 'pointTable';
  this.updaterFunction = this.updateTables; // the container needs to know how to update us
  this.hasChanged = true; // and if we need updating
  // append our nodes to the container
  this.monicaContainer.content.appendChild(this.contentDomNode);
  this.monicaContainer.editor.appendChild(this.editDomNode);

  // add the editing functions
  // the tree of all available monitoring points, taken from the parent MoniCA server
  this.treeSideDiv = dojo.create('div', {'class': 'pointTableTreeSideDiv'});
  this.treeSideDiv.appendChild(dojo.create('p', {innerHTML: 'Available Points'}));
  this.treeDiv = dojo.create('div', {'class': 'pointTableTreeDiv'});
  this.treeControl = new dijit.Tree({
    model: this.monicaServer.treeModel,
    showRoot: false,
    'class': 'pointTreeControl'});
  this.treeDiv.appendChild(this.treeControl.domNode);
  this.treeSideDiv.appendChild(this.treeDiv);
  this.editDomNode.appendChild(this.treeSideDiv);
  this.treeHandle = dojo.connect(this.treeControl, 'onDblClick', this, this.addFromClick);

  // the multi-select box showing what points we currently show
  this.editSelectDiv = dojo.create('div', {'class': 'pointTableEditDiv'});
  this.editSelectDiv.appendChild(dojo.create('p', {innerHTML: 'Points in table'}));
  this.editSelectScrollDiv = dojo.create('div', {'class': 'pointTableEditScrollDiv'});
  this.editSelectDiv.appendChild(this.editSelectScrollDiv);
  this.editSelect = dojo.create('select', {multiple: 'multiple', size: '20',
					   'class': 'pointTableEditSelect'});
  this.editSelectScrollDiv.appendChild(this.editSelect);
  this.editRemoveButton = dojo.create('button', {type: 'button', innerHTML: 'Remove Points',
						 'class': 'pointTableEditRemoveButton'});
  this.editSelectDiv.appendChild(this.editRemoveButton);
  this.removeButtonHandle = dojo.connect(this.editRemoveButton, 'onclick', this, this.removePoints);
  this.removeClickHandle = dojo.connect(this.editSelect, 'ondblclick', this, this.removeFromClick);
  this.editDomNode.appendChild(this.editSelectDiv);
  this.editSelectOptions = []; // we don't yet have any points
}

// some functions for the pointTable
pointTable.prototype.destroy = function() {
  // we need to provide this function for the container to close us correctly

  // get rid of all our points from the MoniCA server
  this.monicaServer.removeUpdatePoint(this.hasPoints);

  // disconnect our buttons
  dojo.disconnect(this.treeHandle);
  dojo.disconnect(this.removeButtonHandle);
  dojo.disconnect(this.removeClickHandle);

};

pointTable.prototype.addFromClick = function(evtObj) {
  // determine the item that was clicked
  var clickedId = this.monicaServer.store.getValue(evtObj, 'id');
  var clickedElements = clickedId.split(/\./);
  // add this element to the points
  var clickedIds = [clickedId];
  this.addPoints(clickedIds);

};

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
      this.editSelectOptions.push(dojo.create('option', {innerHTML: points[i]}));
      this.editSelect.appendChild(this.editSelectOptions[this.editSelectOptions.length - 1]);
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
      headerCell = dojo.create('td', {innerHTML: '<b>' + tableColumns[i][j] + '</b>'});
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
      var pointCell = dojo.create('th', {'class': 'description_' + this.monicaServer.safifyClassName(pointPoints[0]),
					 innerHTML: '&nbsp;'});
      pointRow.appendChild(pointCell);
      // now go through the appropriate points and add them to the
      // appropriate columns
      for (var k = 0; k < tableColumns[i].length; k++) {
	pointCell = dojo.create('td', {innerHTML: '&nbsp;'});
	for (var l = 0; l < pointPoints.length; l++) {
	  var pointElements = pointPoints[l].split(/\./);
	  if (pointElements[0] == tableColumns[i][k]) {
	    dojo.addClass(pointCell, this.monicaServer.safifyClassName(pointPoints[l]));
	    break;
	  }
	}
	pointRow.appendChild(pointCell);
      }
      // now the units cell
      pointCell = dojo.create('td', {'class': 'units_' + this.monicaServer.safifyClassName(pointPoints[0]),
				     innerHTML: '&nbsp;'});
      pointRow.appendChild(pointCell);
      tableNodes[i].appendChild(pointRow);
    }
    this.contentDomNode.appendChild(tableNodes[i]);
  }

  this.monicaServer.getDescriptions();
  this.hasChanged = false;
};


function monicaHTMLFrontPage(options) {

  // make sure we have all the options we need
  if (!options) {
    document.write('New monicaHTMLFrontPage was not called with an options object, you fail!');
    return (null);
  }
  if (!options.topDivId) {
    document.write('Must supply ID of top level element to attach to, as topDivId!');
    return (null);
  }
  if (!options.availableServers) {
    document.write('Must supply list of available MoniCA servers, as availableServers!');
    return (null);
  }

  // set up this object's properties
  this.topDivId = options.topDivId;
  this.availableServers = options.availableServers;
  if (!options.updateTime || options.updateTime < 0) {
    // set the default update time
    this.updateTime = 10; // seconds
  } else {
    this.updateTime = options.updateTime;
  }

  // check whether we should be making a connection immediately
  this.monicaConnection = { serverIndex: -1, serverConnection: null };

  for (var i = 0; i < this.availableServers.length; i++) {
    if (this.availableServers[i].connect &&
	this.availableServers[i].connect == true) {
      this.monicaConnection.serverConnection = new monicaConnection(this.availableServers[i].host);
      this.monicaConnection.serverIndex = i;
      break; // we'll only connect to the first one
    }
  }

  // who are our child containers?
  this.childContainers = [];
  this.childPointTables = [];
  this.childPlots = [];

  // create a popup window for various purposes
  this.dialogPopup = new dijit.Dialog({title: 'MoniCA Dialog'});
  this.dialogPopupDiv = dojo.create('div', {id: 'frontPageDialog'});
  this.dialogPopup.attr('content', this.dialogPopupDiv);
  // we'll use row-hiding tables for this dialog
  this.dialogTable = dojo.create('table');
  this.dialogPopupDiv.appendChild(this.dialogTable);

  // one purpose is to display the link generated for a particular layout
  this.dialogLinkRow = dojo.create('tr');
  this.dialogTable.appendChild(this.dialogLinkRow);
  this.dialogLinkCell = dojo.create('td');
  this.dialogLinkRow.appendChild(this.dialogLinkCell);
  this.dialogLinkCell.appendChild(dojo.create('div', {innerHTML: 'Page link:'}));
  this.dialogLinkBox = dojo.create('textarea', {name: 'dialogLinkBox', cols: '60', rows: '20'});
  this.dialogLinkCell.appendChild(this.dialogLinkBox);

  // execute the draw routine
  this.frontPageHeader = null;
  this.draw();
}

monicaHTMLFrontPage.prototype.connectServer = function(server) {

  // programmatically establish a connection
  // check whether we are already connected to this server
  if (this.monicaConnection.serverConnection) {
    if (this.monicaConnection.serverConnection.monitoringServer == server.host) {
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
  this.monicaConnection.serverConnection = new monicaConnection(this.availableServers[knownHost].host);
  this.monicaConnection.serverIndex = knownHost;

  // update the button states
  this.buttonStates();
};

monicaHTMLFrontPage.prototype.disconnectServer = function() {

  // break the connection
  this.monicaConnection.serverConnection.disconnect();
  this.monicaConnection.serverConnection = null;
  this.monicaConnection.serverIndex = -1;

};

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

  this.frontPageHeader.appendChild(dojo.create('div', {innerHTML: 'MoniCA Web Client Page'}));

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
  this.monicaServersBox = dojo.create('select', {id: 'monicaServerSelection'});
  for (var i = 0; i < this.availableServers.length; i++) {
    this.monicaServersBox.appendChild(dojo.create('option', {value: this.availableServers[i].host,
	    innerHTML: this.availableServers[i].name}));
  }
  var optionsTableCell = dojo.create('td');
  optionsTableCell.appendChild(this.monicaServersBox);
  optionsTableRow.appendChild(optionsTableCell);

  // the time between server updates
  optionsTableRow.appendChild(dojo.create('th', {innerHTML: 'Update Time (s):'}));
  this.updateTimeBox = dojo.create('input', {type: 'text', id: 'updateTimeValue',
					     value: this.updateTime, size: '3'});
  optionsTableCell = dojo.create('td');
  optionsTableCell.appendChild(this.updateTimeBox);
  optionsTableRow.appendChild(optionsTableCell);

  // the buttons to connect/disconnect from a server, and start/stop monitoring
  optionsTableRow = dojo.create('tr');
  optionsTable.appendChild(optionsTableRow);
  optionsTableCell = dojo.create('td', {colspan: '2'});
  optionsTableRow.appendChild(optionsTableCell);
  this.connectorButton = dojo.create('button', {type: 'button'});
  optionsTableCell.appendChild(this.connectorButton);

  optionsTableCell = dojo.create('td', {colspan: '2'});
  optionsTableRow.appendChild(optionsTableCell);
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
  this.newPointTableButton = dojo.create('button', {type: 'button', innerHTML: 'New Point Table'});
  buttonsTableCell.appendChild(this.newPointTableButton);
  buttonsTableCell = dojo.create('td');
  buttonsTableRow.appendChild(buttonsTableCell);
  this.newPlotButton = dojo.create('button', {type: 'button', innerHTML: 'New Time-Series'});
  buttonsTableCell.appendChild(this.newPlotButton);
  buttonsTableCell = dojo.create('td');
  buttonsTableRow.appendChild(buttonsTableCell);
  this.presetButton = dojo.create('button', {type: 'button', innerHTML: 'Load Preset'});
  buttonsTableCell.appendChild(this.presetButton);
  buttonsTableCell = dojo.create('td');
  buttonsTableRow.appendChild(buttonsTableCell);
  this.linkGenerateButton = dojo.create('button', {type: 'button', innerHTML: 'Generate Link'});
  buttonsTableCell.appendChild(this.linkGenerateButton);

  // connect the buttons to their events
  dojo.connect(this.newPointTableButton, 'onclick', this, this.addPointTable);
  dojo.connect(this.newPlotButton, 'onclick', this, this.addPlot);
  dojo.connect(this.linkGenerateButton, 'onclick', this, this.generateLink);

  // make our clearance div
  this.frontPageHeader.appendChild(dojo.create('div', {'class': 'frontPageClearDiv'}));

  // give the buttons their states
  this.buttonStates();

};

monicaHTMLFrontPage.prototype.buttonStates = function() {

  // check and update the states of the start/stop, connect/disconnect buttons
  // and all the action buttons

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

monicaHTMLFrontPage.prototype.updaterHandler = function() {

  // check the current state
  if (this.starterButton.innerHTML == 'Start') {
    // start monitoring
    this.monicaConnection.serverConnection.startMonitoring(this.updateTimeBox.value);
  } else if (this.starterButton.innerHTML == 'Stop') {
    // stop monitoring
    this.monicaConnection.serverConnection.stopMonitoring();
  }

  // update the button's state
  this.buttonStates();

};

monicaHTMLFrontPage.prototype.addPointTable = function() {

  // add a new point table
  // create its container
  this.childContainers.push(new monicaContainer({name: 'Container' + this.childContainers.length,
						 parent: this}));
  // make the table itself
  this.childPointTables.push(new pointTable(this.monicaConnection.serverConnection,
					    this.childContainers[this.childContainers.length - 1]));
  // add the container to the page
  dojo.byId(this.topDivId).appendChild(this.childContainers[this.childContainers.length - 1].domNode);

};

monicaHTMLFrontPage.prototype.addPlot = function() {

  // add a new plot
  // create its container
  this.childContainers.push(new monicaContainer({name: 'Container' + this.childContainers.length,
						 parent: this}));
  // make the plot itself
  this.childPlots.push(new timeSeries(this.monicaConnection.serverConnection,
				      this.childContainers[this.childContainers.length - 1]));
  // add the container to the page
  dojo.byId(this.topDivId).appendChild(this.childContainers[this.childContainers.length - 1].domNode);

};

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
  optionsString += 'server=' + this.monicaConnection.serverConnection.monitoringServer;
  optionsString += '&updateTime=' + this.monicaConnection.serverConnection.updateTime;

  // go through each of our children
  for (var i = 0; i < this.childContainers.length; i++) {
    //    for (var i=0;i<this.childPointTables.length;i++){
    if (this.childContainers[i].childType == 'pointTable') {
      optionsString += '&pointTable=';
      for (var j = 0; j < this.childContainers[i].childObject.hasPoints.length; j++) {
	if (j != 0) {
	  optionsString += ',';
	}
	optionsString += this.childContainers[i].childObject.hasPoints[j];
      }

    } else if (this.childContainers[i].childType == 'timeSeries') {
      //    for (var i=0;i<this.childPlots.length;i++){
      optionsString += '&timeSeries=';
      // the point names
      for (var j = 0; j < this.childContainers[i].childObject.hasPoints.length; j++) {
	if (j != 0) {
	  optionsString += ',';
	}
	optionsString += this.childContainers[i].childObject.hasPoints[j];
      }
      // the time span
      optionsString += ',' + this.childContainers[i].childObject.timeIntervalInput.value;
      // the start time
      if ((this.childContainers[i].childObject.timeNowInput.checked == true) ||
	  (this.childContainers[i].childObject.timeStartBox.value == '')) {
	optionsString += ',-1';
      } else {
	optionsString += ',' + this.childContainers[i].childObject.timeStartBox.value;
      }
      // the number of points returned
      if ((this.childContainers[i].childObject.maxPointsCheckbox.checked == false) ||
	  (this.childContainers[i].childObject.maxPointsInput.value == '')) {
	optionsString += ',-1';
      } else {
	optionsString += ',' + this.childContainers[i].childObject.maxPointsInput.value;
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

function linkParser(options) {
  // this function will parse the current URL and set up the MoniCA pages accordingly

  // options contains the way to set up
  if (!options) {
    document.write('Unable to setup page according to link: no information!');
    return;
  }

  // get the setup information, if any
  var setupInfo = location.search;
  if (setupInfo.length == 0) {
    //	console.log("no setup infomation");
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
	var pointTablePointer = options.frontPage.childPointTables[options.frontPage.childPointTables.length - 1];
	pointTablePointer.addPoints(allPoints);

      } else if (options.topDivId) {
	// we have a blank page to use
	// make a container
	var pointTableContainer = new monicaContainer('pointTable');
	// make the point table
	var pointTableTable = new pointTable(this.monicaConnection, pointTableContainer);
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
	var timeSeriesPointer = options.frontPage.childPlots[options.frontPage.childPlots.length - 1];
	timeSeriesPointer.addPoints(allPoints);
	timeSeriesPointer.setPlotTime(newStartTime, newTimeInterval, newMaxPoints);
	timeSeriesPointer.updatePlot();
      } else if (options.topDivId) {
	// we have a blank page to use
	// make a container
	var timeSeriesContainer = new monicaContainer('timeSeries');
	// make the time series
	var timeSeriesPlot = new pointTable(this.monicaConnection, timeSeriesContainer);
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

linkParser.prototype.nextToken = function(string) {

  var elements = /^\?*(\S+?)\=.*$/.exec(string);
  if (!elements) {
    return (null);
  }
  return (elements[1]);

};

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

function timeSeries(monicaServer,monicaContainer) {
  // the points we are displaying
  this.hasPoints = [];
  // the time span of the series, in minutes
  this.timeSpan = 60; // default 1 hour
  // the start time of the series
  this.startTime = -1; // default is no start time (ie. last timeSpan)

  // the div to contain the rendered time series plot
  this.contentDomNode = dojo.create('div', {'class': 'timeSeriesDiv'});
  this.plotObject = null;
  // the div to contain the editing functions
  this.editDomNode = dojo.create('div', {'class': 'timeSeriesEdit'});

  // the MoniCA server that will update us, and the MoniCA container that holds us
  this.monicaServer = monicaServer;
  this.monicaContainer = monicaContainer;
  // set all the required values in the server and container
  this.monicaContainer.childObject = this; // we are the child of the container
  this.monicaContainer.childType = 'timeSeries';
  this.updaterFunction = this.updatePlot; // the container needs to know how to update us
  this.hasChanged = true; // and if we need updating
  this.pointsChanged = false; // only if a point has been added/removed
  // append our nodes to the container
  this.monicaContainer.content.appendChild(this.contentDomNode);
  this.monicaContainer.editor.appendChild(this.editDomNode);

  // add the editing functions
  // the div to contain the time selection
  this.timeSelectionDomNode = dojo.create('div', {'class': 'plotTimeSelectDiv'});
  this.editDomNode.appendChild(this.timeSelectionDomNode);
  this.timeSelectionTable = dojo.create('table');
  this.timeSelectionDomNode.appendChild(this.timeSelectionTable);
  var timeRow = dojo.create('tr');
  this.timeSelectionTable.appendChild(timeRow);
  var timeCell = dojo.create('th', {innerHTML: 'Plot: '});
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td');
  timeRow.appendChild(timeCell);
  this.timeIntervalInput = dojo.create('input', {type: 'text', size: '5',
						 value: '60'});
  timeCell.appendChild(this.timeIntervalInput);
  timeCell = dojo.create('td', {innerHTML: 'minutes'});
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('th', {innerHTML: 'starting: '});
  timeRow.appendChild(timeCell);
  this.timeNowInput = dojo.create('input', {type: 'radio', name: 'timeStarting',
					    value: 'now', checked: 'true'});
  timeCell = dojo.create('td');
  timeCell.appendChild(this.timeNowInput);
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td', {innerHTML: 'now'});
  timeRow.appendChild(timeCell);
  this.timeStartInput = dojo.create('input', {type: 'radio', name: 'timeStarting',
					      value: 'then'});
  timeCell = dojo.create('td');
  timeCell.appendChild(this.timeStartInput);
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td', {innerHTML: 'time:'});
  timeRow.appendChild(timeCell);
  this.timeStartBox = dojo.create('input', {name: 'startTime', id: 'startTime',
					    type: 'text', size: '20'});
  timeCell = dojo.create('td');
  timeCell.appendChild(this.timeStartBox);
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td');
  timeRow.appendChild(timeCell);
  this.timeStartHREF = "javascript:NewCssCal('startTime','yyyymmdd','dropdown',true,24,false)";
  this.timeStartBoxSelector = dojo.create('a', {href: this.timeStartHREF,
						innerHTML: '<img src="images/cal.gif" width="16" height="16"' +
						' alt="Pick a date">'});
  timeCell.appendChild(this.timeStartBoxSelector);
  timeCell = dojo.create('td');
  timeRow.appendChild(timeCell);
  this.maxPointsCheckbox = dojo.create('input', {type: 'checkbox', name: 'maxPointsCheck',
						 value: 'true', checked: true});
  timeCell.appendChild(this.maxPointsCheckbox);
  timeCell = dojo.create('td', {innerHTML: 'max # points: '});
  timeRow.appendChild(timeCell);
  timeCell = dojo.create('td');
  timeRow.appendChild(timeCell);
  this.maxPointsInput = dojo.create('input', {type: 'text', name: 'maxPoints',
					      size: '5', value: '200'});
  timeCell.appendChild(this.maxPointsInput);
  // connect the radio buttons to the button state updater
  this.timeHandle1 = dojo.connect(this.timeNowInput, 'onclick', this, this.buttonStates);
  this.timeHandle2 = dojo.connect(this.timeStartInput, 'onclick', this, this.buttonStates);
  this.maxPointsHandle = dojo.connect(this.maxPointsCheckbox, 'onclick', this, this.buttonStates);
  this.maxPointsChangeHandle = dojo.connect(this.maxPointsInput, 'onchange', this, this.valuesUpdated);
  this.intervalChangeHandle = dojo.connect(this.timeIntervalInput, 'onchange', this, this.valuesUpdated);
  this.timeChangeHandle = dojo.connect(this.timeStartBox, 'onchange', this, this.valuesUpdated);

  // the tree of all available monitoring points, taken from the parent MoniCA server
  this.treeSideDiv = dojo.create('div', {'class': 'plotTreeSideDiv'});
  this.treeSideDiv.appendChild(dojo.create('p', {innerHTML: 'Available Points'}));
  this.treeDiv = dojo.create('div', {'class': 'plotTreeDiv'});
  this.treeControl = new dijit.Tree({
    model: this.monicaServer.treeModel,
    showRoot: false,
    'class': 'plotTreeControl'});
  this.treeDiv.appendChild(this.treeControl.domNode);
  this.treeSideDiv.appendChild(this.treeDiv);
  this.editDomNode.appendChild(this.treeSideDiv);
  this.treeHandle = dojo.connect(this.treeControl, 'onDblClick', this, this.addFromClick);

  // the multi-select box showing what points we currently show
  this.editSelectDiv = dojo.create('div', {'class': 'plotEditDiv'});
  this.editSelectDiv.appendChild(dojo.create('p', {innerHTML: 'Points in plots'}));
  this.editSelectScrollDiv = dojo.create('div', {'class': 'plotEditScrollDiv'});
  this.editSelectDiv.appendChild(this.editSelectScrollDiv);
  this.editSelect = dojo.create('select', {multiple: 'multiple', size: '20',
					   'class': 'plotEditSelect'});
  this.editSelectScrollDiv.appendChild(this.editSelect);
  this.editRemoveButton = dojo.create('button', {type: 'button', innerHTML: 'Remove Points',
						 'class': 'plotEditRemoveButton'});
  this.editSelectDiv.appendChild(this.editRemoveButton);
  this.removeButtonHandle = dojo.connect(this.editRemoveButton, 'onclick', this, this.removePoints);
  this.removeClickHandle = dojo.connect(this.editSelect, 'ondblclick', this, this.removeFromClick);
  this.editDomNode.appendChild(this.editSelectDiv);
  this.editSelectOptions = []; // we don't yet have any points

  // start the buttons in the right state
  this.buttonStates();
}

timeSeries.prototype.setPlotTime = function(startTime,timeInterval,maxPoints) {
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

timeSeries.prototype.buttonStates = function() {

  // check and update the states of the time select radio buttons
  // and associated inputs

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

timeSeries.prototype.addFromClick = function(evtObj) {
  // determine the item that was clicked
  var clickedId = this.monicaServer.store.getValue(evtObj, 'id');
  var clickedElements = clickedId.split(/\./);
  // add this element to the points
  var clickedIds = [clickedId];
  this.addPoints(clickedIds);

};

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
      this.editSelectOptions.push(dojo.create('option', {innerHTML: points[i]}));
      this.editSelect.appendChild(this.editSelectOptions[this.editSelectOptions.length - 1]);
    }
  }

  // add the points to the update list
  this.monicaServer.addTimeSeriesPoint(allNew, this);

  this.hasChanged = true;
  this.pointsChanged = true;
};

timeSeries.prototype.removeFromClick = function(eventObj) {
  // determine the item that was clicked
  var clickedSelectOption = eventObj.target;

  // remove from the MoniCA list
  this.monicaServer.removeTimeSeriesPoint([clickedSelectOption.innerHTML], this);

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

timeSeries.prototype.valuesUpdated = function(evtObj) {
  this.hasChanged = true;
};

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
  var rangeChanged = this.monicaServer.setTimeSeriesPointRange(this, timeStart, this.timeIntervalInput.value,
							       maxNPoints);

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
      this.plotObject.series[seriesIndex].setData(timeSeriesData[i].data, false);
    }
  }

  // remove any series that have been removed since last update
  for (var i = 0; i < this.plotObject.series.length; i++) {
    var isPresent = -1;
    for (var j = 0; j < this.hasPoints.length; j++) {
      if (this.plotObject.series[i].name == this.modifyName(this.hasPoints[j])) {
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

