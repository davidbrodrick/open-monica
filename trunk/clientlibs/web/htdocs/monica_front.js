dojo.require('dojo.NodeList-traverse');
dojo.require('dojo.data.ItemFileReadStore');
dojo.require('dijit.Tree');
dojo.require('dijit.tree.dndSource');
dojo.require('dijit.form.DateTextBox');

var pointSelector = function(spec, my) {
  /**
   * Some general purpose private variables.
   */
  // In pointsTree.
  var tree, count, i, heirarchy, search, j, k, isFound;
  // In compilePoints.
  var attributes, values, parents;

  /**
   * The object that we will return to our caller.
   * @type {object}
   */
  var that = {};

  // Set some sensible defaults in the spec object.
  /**
   * The list of objects controlling this object.
   * @type {object}
   */
  spec = spec || {};

  /**
   * The ID of the DOM to display our tree in.
   * @type {string}
   */
  spec.treeNode = spec.treeNode || '';

  /**
   * The points we display in our tree.
   * @type {array}
   */
  var allPoints = [];

  /**
   * The tree model for our points.
   * @type {object}
   */
  var pointsTreeModel;

  /**
   * The data store for our model.
   * @type {object}
   */
  var store;

  /**
   * The tree control.
   * @type {object}
   */
  var treeControl;

  /**
   * The point names that the user has clicked on.
   * @type {array}
   */
  var clickedPoints = [];

  // Our methods follow.

  // Our private methods.
  /**
   * Take our list of points and make a Dojo tree model.
   */
  var pointsTree = function() {
    // Parse the point names and create the levels required for
    // the tree model.
    tree = [];
    count = 0;
    for (i = 0; i < allPoints.length; i++) {
      heirarchy = allPoints[i].split(/\./);
      search = tree;
      for (j = 1; j < heirarchy.length; j++) {
	isFound = 0;
	// Search for an existing element.
	for (k = 0; k < search.length; k++) {
	  if (search[k].label === heirarchy[j]) {
	    isFound = 1;
	    search = search[k].children;
	    break;
	  }
	}
	if (isFound === 0) {
	  // Need to create this level.
	  search.push({
	    id: ++count,
	    label: heirarchy[j],
	    children: []
	  });
	  search = search[search.length - 1].children;
	}
      }
      search.push({
	id: allPoints[i],
	label: heirarchy[0],
	pointName: allPoints[i]
      });
    }

    // Prepare the Dojo tree model for all the children.
    store = new dojo.data.ItemFileReadStore({
      data: {
	identifier: 'id',
	label: 'label',
	items: tree
      }
    });

    pointsTreeModel = new dijit.tree.ForestStoreModel({
      store: store
    });

  };

  /**
   * Prepare the Dojo tree for display.
   */
  var makeControl = function() {
    treeControl = new dijit.Tree({
      model: pointsTreeModel,
      showRoot: false,
      'class': 'pointTreeControl',
      id: 'pointTreeControl'
//      dndController: dndSource
    });
  };

  /**
   * Compile a list of points that have been selected by the user.
   * @param {object} evtObj The store item that has been selected.
   */
  var compilePoints = function(evtObj) {
    parents = [];
    clickedPoints = [];

    var adderFn = function(item) {
      var kids = store.getValues(item, 'children');
      if (kids.length > 0) {
	parents.push(item);
      } else {
	clickedPoints.push(store.getValue(item, 'id'));
      }
    };

    // Start with the point that was clicked.
    adderFn(evtObj);

    // Go through all the parents we know about.
    while (parents.length > 0) {
      var kiddies = store.getValues(parents[0], 'children');
      for (i = 0; i < kiddies.length; i++) {
	adderFn(kiddies[i]);
      }
      parents.splice(0, 1);
    }

    dojo.publish('pointsSelected', [ clickedPoints ]);
  };

  // Our public methods.
  /**
   * Add a list of point names to display in the tree.
   * @param {array} pointNames The names of points to display in the tree.
   */
  that.addPoints = function(pointNames) {

  };

  /**
   * Set the point names to be this list, overwriting any current points.
   * @param {array} pointNames The names of points to display in the tree.
   */
  that.setPoints = function(pointNames) {
    allPoints = pointNames;
    // Refresh the model.
    pointsTree();
  };

  /**
   * Attach a Dojo tree control to the named ID, or to the default set
   * when we were made.
   * @param {string} nodeId The node to attach the tree display to.
   */
  that.attachTree = function(nodeId) {
    nodeId = nodeId || spec.treeNode;

    // Make the control.
    makeControl();

    // Attach it to the node.
    dojo.byId(nodeId).appendChild(treeControl.domNode);

    // Activate it for use.
    dojo.connect(treeControl, 'onDblClick',
      function (evtObj) {
	// Get a list of points to add.
	compilePoints(evtObj);
      }
    );
  };

  return that;
};

var editableText = function(spec, my) {
  /**
   * Some general purpose private variables
   */
  var i, temp;

  /**
   * The object that we will return to our caller.
   * @type {object}
   */
  var that = {};

  // Set some sensible defaults in the spec object.
  /**
   * The list of objects controlling this object.
   * @type {object}
   */
  spec = spec || {};

  /**
   * The IDs of the DOM element to attach to.
   * @type {string}
   */
  spec.editableNode = spec.editableNode || '';
  spec.referenceNode = spec.referenceNode || spec.editableNode;

  /**
   * The activity on the reference node which will trigger the edit.
   * @type {string}
   */
  spec.editAction = spec.editAction || 'onclick';

  /**
   * Our editable node.
   * @type {DOMnode}
   */
  var editNode;

  /**
   * The current state of the editable node.
   * @type {number}
   */
  var state = 0; // 0 = not-editing, 1 = editing

  /**
   * The current connection handler.
   * @type {handler}
   */
  var connector = null;

  /**
   * Some keycodes we don't like when we're in editing mode.
   * @type {array}
   */
  var stopCodes = [dojo.keys.ALT, dojo.keys.CLEAR, dojo.keys.CTRL,
      dojo.keys.F1, dojo.keys.F10, dojo.keys.F11,
      dojo.keys.F12, dojo.keys.F13, dojo.keys.F14,
      dojo.keys.F15, dojo.keys.F2, dojo.keys.F3,
      dojo.keys.F4, dojo.keys.F5, dojo.keys.F6,
      dojo.keys.F7, dojo.keys.F8, dojo.keys.F9,
      dojo.keys.HELP, dojo.keys.LEFT_WINDOW,
      dojo.keys.PAGE_DOWN, dojo.keys.PAGE_UP,
      dojo.keys.PAUSE, dojo.keys.RIGHT_WINDOW];

  /**
   * Some keycodes we ignore when we're in editing mode.
   * @type {array}
   */
  var ignoreCodes = [dojo.keys.CAPS_LOCK, dojo.keys.END,
      dojo.keys.DOWN_ARROW, dojo.keys.INSERT,
      dojo.keys.HOME, dojo.keys.INSERT,
      dojo.keys.LEFT_ARROW, dojo.keys.NUM_LOCK,
      dojo.keys.RIGHT_ARROW, dojo.keys.SCROLL_LOCK,
      dojo.keys.SELECT, dojo.keys.SHIFT,
      dojo.keys.TAB, dojo.keys.UP_ARROW];

  // Our methods follow.

  // Our private methods.
  var connect = function() {
    if (spec.referenceNode === '' ||
	spec.editAction === '') {
      return;
    }

    if (state === 0) {
      if (connector !== null) {
	dojo.disconnect(connector);
      }
      connector = dojo.connect(spec.referenceNode, spec.editAction,
	function() {
	  temp = dojo.attr(spec.editableNode, 'innerHTML');
	  editNode = dojo.create('input',
	    {
	      type: 'text',
	      value: temp
	    }
	  );
	  dojo.place(editNode, spec.editableNode, 'only');
	  state = 1;
	  connect();
	}
      );
    } else {
      if (connector !== null) {
	dojo.disconnect(connector);
      }
      connector = dojo.connect(spec.editableNode, 'onkeydown',
	function(evtObj) {
	  // Check for keycodes to stop.
	  for (i = 0; i < stopCodes.length; i++) {
	    if (evtObj.keyCode === stopCodes[i]) {
	      dojo.stopEvent(evtObj);
	      return;
	    }
	  }
	  // Check for keycodes to ignore.
	  for (i = 0; i < ignoreCodes.length; i++) {
	    if (evtObj.keyCode === ignoreCodes[i]) {
	      return;
	    }
	  }
	  // Check for ENTER
	  if (evtObj.keyCode === dojo.keys.ENTER ||
	      evtObj.keyCode === dojo.keys.NUMPAD_ENTER) {
	    temp = dojo.attr(editNode, 'value');
	    dojo.empty(spec.editableNode);
	    dojo.attr(spec.editableNode, 'innerHTML', temp);
	    state = 0;
	    connect();
	    return;
	  }
	}
      );
    }
  };

  // Our public methods.

  // Make the connection.
  connect();

  return that;
};

var timeSeries = function(spec, my) {
  /**
   * Some general purpose private variables
   */
  var i;
  // In addPoint.
  var pointFound, newPoint;
  // In findSeries.
  var fSi;
  // In addSeries2Plot.
  var iSeries;
  // In internal2Chart.
  var oRefs, i2Ci, oObjs, tDetails, tName;
  // In makePlotArea.
  var mSeries, pRef;
  // In addSeries.
  var aSi;
  // In updatePlot.
  var pDetails, nv;

  /**
   * The object that we will return to our caller.
   * @type {object}
   */
  var that = {};

  // Set some sensible defaults in the spec object.
  /**
   * The list of objects controlling this object.
   * @type {object}
   */
  spec = spec || {};

  /**
   * The ID of the DOM element we will attach our plot to.
   * @type {string}
   */
  spec.plotDOM = spec.plotDOM || '';

  /**
   * The ID of the DOM element we will put our options in.
   * @type {string}
   */
  spec.optionsDOM = spec.optionsDOM || '';

  /**
   * Which plotting library do we use? Options are:
   * highstock: the HighCharts Stock library (default)
   * highcharts: the HighCharts library
   * @type {string}
   */
  spec.plottingLibrary = spec.plottingLibrary || 'highstock';

  /**
   * The options for our series. We keep it consistent with what the
   * MoniCA server expects for time series.
   * @type {object}
   */
  var seriesOptions = {
    spanTime: 60, // in minutes, default 60 minutes
    startTime: -1, // default is no start time (ie. last timeSpan)
    maxPoints: 60 // default no limit to the number of points
  };

  // Our plot object.
  var plotObject = null;

  // The time series we handle.
  var ourSeries = [];

  // Our methods follow.

  // Our private methods.
  /**
   * Add a point to the series that we handle.
   * @param {string} pointName The name of the point to add.
   */
  var addPoint = function(pointName) {
    // Check we haven't already got this point.
    pointFound = findSeries(pointName);
    if (pointFound !== null) {
      return;
    }

    // Add the point to the list.
    newPoint = {
      name: pointName,
      chartSeries: null,
      monicaRef: null
    };
    ourSeries.push(newPoint);
  };

  /**
   * Find and return the series object with the specified name.
   * @param {string} searchName The name of the series to return.
   */
  var findSeries = function(searchName) {
    for (fSi = 0; fSi < ourSeries.length; fSi++) {
      if (ourSeries[fSi].name === searchName) {
	return ourSeries[fSi];
      }
    }

    // If we get here, we did not find an appropriate series,
    // so we return 'null'.
    return null;
  };

  /**
   * Add a new series to the plot.
   * @param {object} aSeries The series to add.
   */
  var addSeries2Plot = function(aSeries) {
    // Check that we actually have a plot to add to.
    if (plotObject === null) {
      // We have no plot, so we make one.
      makePlotArea();
    } else {
      // We add our series.
      iSeries = internal2Chart(aSeries);
      aSeries.chartSeries = plotObject.addSeries(
	iSeries[0], // The series to add to the plot.
	true, // A flag telling the plot to redraw.
	false // A flag indicating we don't want animation.
      );
    }
  };

  /**
   * Turn our internal representation of our series into the
   * format that the plot requires.
   * @param {object} sRef An optional reference to the series that should
   *                      be translated. If not specified, all eligible
   *                      series will be returned.
   */
  var internal2Chart = function(sRef) {
    // The array of references to convert.
    oRefs = [];
    if (sRef) {
      // Just add the supplied references.
      oRefs.push(sRef);
    } else {
      // Go through and determine which series are ready to be added to
      // our plot.
      for (i2Ci = 0; i2Ci < ourSeries.length; i2Ci++) {
	if (ourSeries[i2Ci].monicaRef.timeSeriesInitialised() === true &&
	    ourSeries[i2Ci].chartSeries === null) {
	  // This plot has data and hasn't yet been added to the plot.
	  oRefs.push(ourSeries[i2Ci]);
	}
      }
    }

    // Go through the references and make the correct objects.
    oObjs = [];
    for (i2Ci = 0; i2Ci < oRefs.length; i2Ci++) {
      tDetails = oRefs[i2Ci].monicaRef.getPointDetails();
      tName = tDetails.description;
      if (tDetails.units !== '') {
	tName += ' [' + tDetails.units + ']';
      }
      oObjs.push({
	name: tName,
	id: tDetails.name,
	data: oRefs[i2Ci].monicaRef.getTimeSeries()
      });
    }

    return oObjs;
  };

  /**
   * Make the plot area.
   */
  var makePlotArea = function() {
    // Set up the Highcharts global options.
    if (spec.plottingLibrary === 'highcharts' ||
	spec.plottingLibrary === 'highstock') {
      Highcharts.setOptions({
	global: {
	  useUTC: true
	}
      });
    }

    // Compile the series that we will begin with.
    mSeries = internal2Chart();

    // Make the plot object.
    if (spec.plottingLibrary === 'highstock') {
      plotObject = new Highcharts.StockChart({
	chart: {
	  renderTo: spec.plotDOM,
	  height: 300,
	  animation: false
	},
	plotOptions: {
	  series: {
	    animation: false
	  }
	},
	title: {
	  text: 'MoniCA plot'
	},
	xAxis: {
	  title: {
	    text: 'Time (UT)'
	  }
	},
	yAxis: {
	  title: {
	    text: 'Value'
	  }
	},
	series: mSeries
      });
    } else if (spec.plottingLibrary === 'highcharts') {
      plotObject = new Highcharts.Chart({
	chart: {
	  renderTo: spec.plotDOM,
	  height: 300,
	  defaultSeriesType: 'line',
	  animation: false
	},
	plotOptions: {
	  series: {
	    animation: false
	  }
	},
	title: {
	  text: 'MoniCA plot'
	},
	xAxis: {
	  type: 'datetime',
	  title: {
	    enabled: true,
	    text: 'Time (UTC)',
	    startOnTick: false,
	    endOnTick: false,
	    showLastLabel: true
	  }
	},
	yAxis: {
	  title: {
	    text: 'Value'
	  }
	},
	series: mSeries
      });
    }

    // Get the series references for the series we just added.
    if (spec.plottingLibrary === 'highcharts' ||
	spec.plottingLibrary === 'highstock') {
      for (i = 0; i < mSeries.length; i++) {
	pRef = findSeries(mSeries[i].id);
	pRef.chartSeries = plotObject.get(mSeries[i].id);
      }
    }
  };

  /**
   * Add a point to the time series that we handle.
   * @param {string} pointName The name of the point to add.
   */
  that.addSeries = function(pointName) {
    // Add the point using our own private method.
    for (aSi = 0; aSi < pointName.length; aSi++) {
      addPoint(pointName[aSi]);
    }
  };

  /**
   * Return our type.
   */
  that.type = function() {
    return 'timeSeries';
  };

  /**
   * Return the options controlling the time series, formatted as required
   * for the MoniCA server.
   */
  that.getTimeSeriesOptions = function() {
    return seriesOptions;
  };

  /**
   * Update the plot.
   * @param {object} pointRef A reference to the point that has been updated.
   */
  that.updatePlot = function(pointRef) {
    // Get the details about the point we've been called for.
    pDetails = pointRef.getPointDetails();
    // Find the appropriate series.
    pointFound = findSeries(pDetails.name);
    if (pointFound === null) {
      // We don't actually handle this point, so return.
      return;
    }
    // Check for a MoniCA reference for our point.
    if (pointFound.monicaRef === null) {
      pointFound.monicaRef = pointRef;
    }

    // Has this series been added to the plot?
    if (pointFound.chartSeries === null) {
      // Add the series to the plot now.
      addSeries2Plot(pointFound);
    } else {
      // We only need to get the latest data point.
      nv = pointRef.latestValue({
	valueAsDecimalDegrees: true
      });
      // Add the new value to the plot.
      pointFound.chartSeries.addPoint(
	nv, // The value to add.
	true, // A flag telling the plot to redraw.
	true, // A flag indicating we want to pop off the first value.
	false // A flag indicating we don't want animation.
      );
    }
  };

  /**
   * Return the function that updates our plot.
   */
  that.callbackFn = function() {
    return that.updatePlot;
  };

  return that;
};

var pointTable = function(spec, my) {
  /**
   * Some general purpose private variables.
   */
  // In makeTable.
  var tableNodes, tableCollection, tableColumns, tablePoints;
  var i, j, k, l, pointElements, pointPrefix, pointPattern, needNew;
  var newColumn, newPoint, headerRow, headerCell, pointRow;
  var pointPoints, matchPattern, pointCell;
  // In updateTable.
  var pDetails, idPrefix, pState, oState;
  // In addPoint.
  var added;
  // In addPoints.
  var pn;

  /**
   * The object that we will return to our caller.
   * @type {object}
   */
  var that = {};

  // Set some sensible defaults in the spec object.
  /**
   * The list of objects controlling this object.
   * @type {object}
   */
  spec = spec || {};

  /**
   * The ID of the DOM element we will build our table in.
   * @type {string}
   */
  spec.tableDOM = spec.tableDOM || '';

  /**
   * The ID of the DOM element we will put our options in.
   * @type {string}
   */
  spec.optionsDOM = spec.optionsDOM || '';

  /**
   * The table we make.
   * @type {DOMnode}
   */
  var table;

  /**
   * The points we handle.
   * @type {array}
   */
  var ourPoints = [];

  /**
   * Our tables.
   * @type {array}
   */
  var ourTables = [];

  // Our methods follow.

  // Our private methods.
  /**
   * Take a point name and turn it into an ID that can be assigned
   * to a DOM element.
   * @param {string} pointName The name of the point.
   */
  var makeSafeId = function(pointName) {
    return pointName.replace(/\./g, '_');
  };

  /**
   * Return the table with the named column or matching point pattern.
   * @param {string} pointName The name of the point to find a table for.
   */
  var whichTable = function(pointName) {
    var rObj = {
      table: null,
      column: -1,
      row: null
    };

    // Find the point pattern and the column name.
    pointElements = pointName.split(/\./);
    pointPattern = /^(\D+)(\d*)(.*)$/i.exec(pointElements[0]);
    pointPrefix = pointElements[1];
    for (i = 2; i < pointElements.length; i++) {
      pointPrefix += '.' + pointElements[i];
    }

    for (i = 0; i < ourTables.length; i++) {
      if (ourTables[i].pointPattern[1] === pointPattern[1]) {
	rObj.table = ourTables[i];
	for (j = 0; j < ourTables[i].columns.length; j++) {
	  if (ourTables[i].columns[j] === pointElements[0]) {
	    rObj.column = j + 1;
	  }
	}
	for (k = 0; k < ourTables[i].rows.length; k++) {
	  if (ourTables[i].rows[k].prefix === pointPrefix) {
	    rObj.row = ourTables[i].rows[k];
	  }
	}
      }
    }

    return rObj;
  };

  /**
   * Make a table cell DOM.
   */
  var makeTableCell = function() {
    return (dojo.create('td',
      {
	innerHTML: '&nbsp;'
      })
    );
  };

  /**
   * Add a new column to a table.
   * @param {object} tTable The table to add a column to.
   * @param {string} columnName The name of the column to add.
   */
  var newTableColumn = function(tTable, columnName) {

    // Do we already know this table's point pattern?
    if (tTable.pointPattern === null) {
      // Determine the point pattern.
      tTable.pointPattern = /^(\D+)(\d*)(.*)$/i.exec(columnName);
    }

    // Add the column to the table, and resort.
    tTable.columns.push(columnName);
    tTable.columns.sort();

    // Which column number is it now?
    var nCol = -1;
    for (i = 0; i < tTable.columns.length; i++) {
      if (tTable.columns[i] === columnName) {
	nCol = i + 1;
      }
    }

    // Add the new column to each row in the table.
    for (i = 0; i < tTable.rows.length; i++) {
      tTable.rows[i].columns.splice(nCol, 0, makeTableCell());
      dojo.place(tTable.rows[i].columns[nCol],
		 tTable.rows[i].columns[nCol - 1], 'after');
    }

    // Add a new column heading.
    tTable.headerRow.columns.splice(nCol, 0, makeTableCell());
    dojo.attr(tTable.headerRow.columns[nCol], 'innerHTML',
	      columnName);
    dojo.addClass(tTable.headerRow.columns[nCol], 'columnName');
    dojo.place(tTable.headerRow.columns[nCol],
	       tTable.headerRow.columns[nCol - 1], 'after');

    return nCol;
  };

  /**
   * Make a new row in the table, based on the first point it will hold.
   * @param {object} tTable The table to add a row to.
   * @param {string} templatePointName The point name that provides the
   *                                   template for the row.
   */
  var newTableRow = function(tTable, templatePointName) {

    pointElements = templatePointName.split(/\./);
    pointPattern = /^(\D+)(\d*)(.*)$/i.exec(pointElements[0]);
    pointPrefix = pointElements[1];
    for (i = 2; i < pointElements.length; i++) {
      pointPrefix += '.' + pointElements[i];
    }

    // Make a new row.
    var tRow = {
      prefix: pointPrefix,
      domNode: dojo.create('tr'),
      columns: []
    };

    // Add the row to the table.
    tTable.rows.push(tRow);
    // And to the DOM.
    tTable.domNode.appendChild(tRow.domNode);

    // Make the right number of columns in the row.
    // The first cell is the description header.
    tRow.columns.push(dojo.create('th',
      {
	id: makeSafeId(templatePointName) + 'Description',
	innerHTML: '&nbsp;'
      })
    );

    // A column for each point.
    for (i = 0; i < tTable.columns.length; i++) {
      tRow.columns.push(makeTableCell());
    }

    // A column for the units.
    tRow.columns.push(dojo.create('td',
      {
	id: makeSafeId(templatePointName) + 'Units',
	innerHTML: '&nbsp;'
      })
    );

    // Add all the columns to row DOM.
    for (i = 0; i < tRow.columns.length; i++) {
      tRow.domNode.appendChild(tRow.columns[i]);
    }

    return tRow;
  };

  /**
   * Add a new table to our collection.
   */
  var newTable = function() {
    var nTable = {
      columns: [],
      pointPattern: null,
      rows: [],
      headerRow: {
	domNode: null,
	columns: []
      },
      domNode: null
    };
    ourTables.push(nTable);

    // Make a new DOM node for our table.
    nTable.domNode = dojo.create('table',
      {
	'class': 'pointTable'
      }
    );

    // Make a new header row.
    nTable.headerRow.domNode = dojo.create('tr');
    // The first header column is blank.
    nTable.headerRow.columns.push(makeTableCell());
    dojo.addClass(nTable.headerRow.columns[0], 'noborder');
    nTable.headerRow.domNode.appendChild(
      nTable.headerRow.columns[0]
    );
    nTable.domNode.appendChild(nTable.headerRow.domNode);

    // Attach the table to the DOM.
    dojo.byId(spec.tableDOM).appendChild(nTable.domNode);

    return nTable;
  };

  /**
   * Modify our tables for a new point.
   */
  var addPoint2Table = function(newPoint) {
    var needsNewTable = false;

    var aTable = whichTable(newPoint);
    if (aTable.table === null) {
      // We need a new table.
      aTable.table = newTable();
    }

    if (aTable.column === -1) {
      // We need a new column.
      pointElements = newPoint.split(/\./);
      aTable.column = newTableColumn(aTable.table, pointElements[0]);
    }

    if (aTable.row === null) {
      // We need a new row.
      aTable.row = newTableRow(aTable.table, newPoint);
    }

    // We describe it appropriately.
    if (aTable.table !== null &&
	aTable.column !== -1 &&
	aTable.row !== null) {
      dojo.attr(aTable.row.columns[aTable.column], 'id',
	makeSafeId(newPoint));
    }
  };

  /**
   * Add a point to our list.
   * @param {string} pointName The name of the point to add.
   */
  var addPoint = function(pointName) {
    // Check it's not already on our list.
    added = 0;
    for (j = 0; j < ourPoints.length; j++) {
      if (ourPoints[j] === pointName) {
	added = 1;
	break;
      }
    }

    // Add the point if it's not already there.
    if (added === 0) {
      ourPoints.push(pointName);
    }

    // Update the table.
    addPoint2Table(pointName);
  };

  // Our public methods.
  /**
   * This routine is called by the MoniCA server as a callback when
   * a point has updated.
   * @param {object} pointRef A reference to the point we can use to get
   *                          more details about it.
   */
  that.updateTable = function(pointRef) {
    pDetails = pointRef.getPointDetails();
    idPrefix = makeSafeId(pDetails.name);
    pState = pointRef.latestValue();
    dojo.attr(idPrefix, 'innerHTML', pState.value);
    if (dojo.byId(idPrefix + 'Description')) {
      dojo.attr(idPrefix + 'Description', 'innerHTML', pDetails.description);
    }
    if (dojo.byId(idPrefix + 'Units')) {
      dojo.attr(idPrefix + 'Units', 'innerHTML', pDetails.units);
    }
    if (pState.errorState === false) {
      // This is an error condition.
      dojo.addClass(idPrefix, 'inError');
    } else {
      dojo.removeClass(idPrefix, 'inError');
    }
  };

  /**
   * Add some new points to our table.
   * @param {array} points An array of point names to handle.
   */
  that.addPoints = function(points) {
    for (pn = 0; pn < points.length; pn++) {
      addPoint(points[pn]);
    }
  };

  /**
   * Return the function that updates our table.
   */
  that.callbackFn = function() {
    return that.updateTable;
  };

  /**
   * Return our type.
   */
  that.type = function() {
    return 'pointTable';
  };

  return that;
};

var displayHandler = function(spec, my) {
  /**
   * Some general purpose private variables.
   */
  // In addContainer.
  var idString, newDiv;
  // In setupContainer.
  var activeDiv, activeId, titleId, titleDiv, contentId, contentDiv;
  var actionsId, actionsDiv, leftDiv, editableId, editableSpan;
  var optionsId, optionsSpan, editHandler;
  // In generateId
  var suffix;
  // In add.
  var newId;
  // In assignActiveObject
  var i;

  /**
   * The object that we will return to our caller.
   * @type {object}
   */
  var that = {};

  // Set some sensible defaults in the spec object.
  /**
   * The list of objects controlling this object.
   * @type {object}
   */
  spec = spec || {};

  /**
   * The name of the DOM element to place our new displays
   * before.
   * @type {string}
   */
  spec.referenceNode = spec.referenceNode || 'adder';

  /**
   * The class to assign to new containers.
   * @type {string}
   */
  spec.containerClass = spec.containerClass || 'displayContainer';

  /**
   * All the displays we are handling.
   * @type {array}
   */
  var myDisplays = [];

  /**
   * The number of containers we have.
   * @type {number}
   */
  var nContainers = 0;

  /**
   * The ID of the active display.
   * @type {string}
   */
  var currentActive = '';

  /**
   * The objects to return as the active objects.
   * @type {array}
   */
  var activeObjects = [];

  // Our methods follow.

  // Our private methods.
  /**
   * Add a container for the display.
   */
  var addContainer = function() {
    // Make a new ID.
    idString = 'displayContainer' + nContainers;
    // Increment this counter for the next time.
    nContainers++;

    // Make the div.
    newDiv = dojo.create('div',
      {
	id: idString,
	'class': spec.containerClass
      }
    );
    // And push it on to our displays list.
    myDisplays.push(idString);
    activeObjects.push({});

    // Add it to the page.
    dojo.place(newDiv, spec.referenceNode, 'before');

    // Return the ID of this new node.
    return idString;
  };

  /**
   * Set up a display container.
   * @param {string} container The ID of the container to set up.
   */
  var setupContainer = function(container) {
    // The region on the right that will show which display is active.
    activeId = generateId(container, 'active');
    activeDiv = dojo.create('div',
      {
	id: activeId,
	'class': 'containerInactive'
      }
    );
    dojo.byId(container).appendChild(activeDiv);

    leftDiv = dojo.create('div',
      {
	'class': 'containerBounds'
      }
    );
    dojo.byId(container).appendChild(leftDiv);

    // The title bar.
    titleId = generateId(container, 'title');
    titleDiv = dojo.create('div',
      {
	id: titleId,
	'class': 'containerTitle'
      }
    );
    leftDiv.appendChild(titleDiv);

    // Put an editable span in the title bar.
    editableId = generateId(container, 'editable');
    editableSpan = dojo.create('span',
      {
	id: editableId,
	'class': 'titleEditable',
	innerHTML: 'Click to edit title'
      }
    );
    titleDiv.appendChild(editableSpan);
    // Make it editable.
    editHandler = editableText({
      editableNode: editableSpan
    });

    // Provide an options area that can be filled with options
    // specific to the display later.
    optionsId = generateId(container, 'options');
    optionsSpan = dojo.create('span',
      {
	id: optionsId,
	'class': 'titleOptions'
      }
    );
    titleDiv.appendChild(optionsSpan);


    // The content area.
    contentId = generateId(container, 'content');
    contentDiv = dojo.create('div',
      {
	id: contentId,
	'class': 'containerContent'
      }
    );
    leftDiv.appendChild(contentDiv);

    // Action buttons.
    actionsId = generateId(container, 'actions');
    actionsDiv = dojo.create('div',
      {
	id: actionsId,
	'class': 'containerActions'
      }
    );
    leftDiv.appendChild(actionsDiv);

    // Setup the action handlers.
    dojo.connect(activeDiv, 'onclick',
      function() {
	activateDisplay(container);
      }
    );
  };

  /**
   * Generate the ID name for a DIV of some type.
   * @param {string} idName The ID of the container.
   * @param {string} type The type of the container.
   */
  var generateId = function(idName, type) {
    switch (type) {
    case 'active':
      suffix = 'Active';
      break;
    case 'title':
      suffix = 'Title';
      break;
    case 'content':
      suffix = 'Content';
      break;
    case 'actions':
      suffix = 'Actions';
      break;
    case 'editable':
      suffix = 'Editable';
      break;
    case 'options':
      suffix = 'Options';
      break;
    default:
      suffix = '';
    }
    return (idName + suffix);
  };

  /**
   * Deactivate the currently active tab.
   */
  var deactivateCurrent = function() {
    if (currentActive !== '') {
      dojo.removeClass(generateId(currentActive, 'active'),
		       'containerActive');
      dojo.addClass(generateId(currentActive, 'active'),
		    'containerInactive');
      currentActive = '';
    }
  };

  /**
   * Activate the named display.
   * @param {string} container The ID of the container to activate.
   */
  var activateDisplay = function(container) {
    activeId = generateId(container, 'active');
    if (activeId === currentActive) {
      return;
    }
    deactivateCurrent();
    dojo.removeClass(activeId, 'containerInactive');
    dojo.addClass(activeId, 'containerActive');
    currentActive = container;
  };

  // Our public methods.
  /**
   * Add a new display.
   */
  that.add = function() {
    newId = addContainer();
    setupContainer(newId);
    activateDisplay(newId);
    return newId;
  };

  /**
   * Assign an object that we will return when asked.
   * @param {string} assignId The ID of the display to assign the object to.
   * @param {object} assignObject The object to return later.
   */
  that.assignActiveObject = function(assignId, assignObject) {
    // Find the ID.
    if (assignId === '') {
      // Use the currently active ID.
      assignId = currentActive;
    }
    for (i = 0; i < myDisplays.length; i++) {
      if (myDisplays[i] === assignId) {
	activeObjects[i] = assignObject;
      }
    }
  };

  /**
   * Return the object for the currently active display.
   */
  that.activeObject = function() {
    for (i = 0; i < myDisplays.length; i++) {
      if (myDisplays[i] === currentActive) {
	return activeObjects[i];
      }
    }
    return null;
  };

  /**
   * Return the index of the currently active display, so other routines
   * can keep their IDs consistent.
   */
  that.activeIndex = function() {
    for (i = 0; i < myDisplays.length; i++) {
      if (myDisplays[i] === currentActive) {
	return i;
      }
    }
    return -1;
  };

  /**
   * Return the DOM ID of the content node for the active display.
   */
  that.activeContent = function() {
    return generateId(currentActive, 'content');
  };

  /**
   * Return the DOM ID of the options node for the active display.
   */
  that.activeOptions = function() {
    return generateId(currentActive, 'options');
  };

  return that;
};

function init() {
  // Set up a MoniCA object.
  var setupOptions = {
    updateInterval: 4000
  };
  var monica = monicaServer(setupOptions);

  // Some subroutines we'll need later

  // Set up a routine to call when we get the point names.
  var pointsKnown = function(serverInfo) {
    // We make a point tree a put it on the page so the user
    // can select which points to put into the view objects.
    var allPoints = monica.pointsList();

    var treeDisplay = pointSelector();
    treeDisplay.setPoints(allPoints);
    treeDisplay.attachTree('right');

    dojo.subscribe('pointsSelected',
      function (pointsList) {
	var dType = displays.activeObject().type();
	if (dType === 'pointTable') {
	  // Add the points to the point table.
	  displays.activeObject().addPoints(pointsList);

	  // Ask MoniCA to update these points.
	  var pointRefs = monica.addPoints(pointsList);
	  for (var i = 0; i < pointsList.length; i++) {
	    // Add a callback for this point.
	    pointRefs[i].addCallback(displays.activeObject().callbackFn());
	  }

	  // Ask MoniCA to get the descriptions and start updating.
	  monica.getDescriptions();
	} else if (dType === 'timeSeries') {
	  // Add the points to the time series.
	  displays.activeObject().addSeries(pointsList);

	  // Ask MoniCA to update these points.
	  for (var i = 0; i < pointsList.length; i++) {
	    var seriesRef = monica.addTimeSeries({
	      pointName: pointsList[i],
	      timeSeriesOptions: displays.activeObject().getTimeSeriesOptions()
	    });

	    // Add a callback for this point.
	    seriesRef.addCallback(displays.activeObject().callbackFn());
	  }

	  // Ask MoniCA to get the descriptions and start updating.
	  monica.getDescriptions();
	}
      }
    );

    monica.startUpdating();
  };

  var setupAdders = function() {
    // Make the buttons required to add new displays.
    var adderDiv = dojo.create('div',
      {
	id: 'adder',
	'class': 'adder'
      }
    );
    dojo.byId('left').appendChild(adderDiv);
    var addPointTable = dojo.create('button',
      {
	type: 'button',
	innerHTML: 'Add Point Table',
	'class': 'addButton'
      }
    );
    adderDiv.appendChild(addPointTable);
    var addTimeSeries = dojo.create('button',
      {
	type: 'button',
	innerHTML: 'Add Time Series',
	'class': 'addButton'
      }
    );
    adderDiv.appendChild(addTimeSeries);

    // Make the buttons do things.
    dojo.connect(addPointTable, 'onclick',
      function() {
	displays.add();
	displays.assignActiveObject('',
	  pointTable({
	    tableDOM: displays.activeContent()
	  })
	);
      }
    );

    dojo.connect(addTimeSeries, 'onclick',
      function() {
	displays.add();
	displays.assignActiveObject('',
	  timeSeries({
	    plotDOM: displays.activeContent(),
	    plottingLibrary: 'highcharts'
	  })
	);
	// Add the option elements we use to control the time series.
	var cOptions = displays.activeObject().getTimeSeriesOptions();
	var aId = 'timeSeries' + displays.activeIndex() + 'Option';
	dojo.place(
	  '<br /><span class="optionsTitle">Options:</span>',
	  displays.activeOptions(), 'last'
	);
	dojo.place(
	  '<label for="' + aId + 'SpanTime">Time Span (mins):</label>' +
	  '<input type="text" id="' + aId + 'SpanTime" ' +
	    'name="' + aId + 'SpanTime" value="' +
	    cOptions.spanTime + '" size="5" />',
	  displays.activeOptions(), 'last'
	);
	dojo.place(
	  '<input type="checkbox" name="' + aId + 'StartTimeNow" ' +
	    'id="' + aId + 'StartTimeNow" checked="yes" />' +
	    '<label for="' + aId + 'StartTimeNow">ending now' +
	    '</label>' +
	    '<label for="' + aId + 'StartTimeDate"> / starting (UTC): ' +
	    '</label>' +
	    '<input id="' + aId + 'StartTimeDate" ' +
	    'name="' + aId + 'StartTimeDate" />' +
	    '<input id="' + aId + 'StartTimeTime" size="9" />',
	  displays.activeOptions(), 'last'
	);
	// Enable the date picker.
	var todaysDate = new Date();
	new ISODateTextBox({
	  value: dojo.date.locale.format(todaysDate, isoFormatter),
	  name: aId + 'StartTimeDatePicker'
	}, aId + 'StartTimeDate');
	var dHours = (todaysDate.getUTCHours() < 10) ?
	  '0' + todaysDate.getUTCHours() : todaysDate.getUTCHours();
	var dMinutes = (todaysDate.getUTCMinutes() < 10) ?
	  '0' + todaysDate.getUTCMinutes() : todaysDate.getUTCMinutes();
	dojo.attr(aId + 'StartTimeTime', 'value',
	  dHours + ':' + dMinutes + ':00');
	if (cOptions.startTime === -1) {
	  // Disable the
	}
      }
    );

  };

  // Subscribe to a MoniCA connection.
  dojo.subscribe('connection', pointsKnown);

  // Initialise the connection.
  monica.connect();

  // Get a new display handler.
  var displays = displayHandler({
    referenceNode: 'adder'
  });

  // Make a customised Dojo calendar.
  var isoFormatter = {
    selector: 'date',
    datePattern: 'yyyy-MM-dd',
    locale: 'en-au'
  };
  dojo.declare('ISODateTextBox', dijit.form.DateTextBox,
    {
      isoFormat: isoFormatter,
      value: '',
      postMixInProperties: function() {
	this.inherited(arguments);
	// Convert our value to the Date object.
	this.value = dojo.date.locale.parse(this.value,
					    this.isoFormat);
      },
      serialize: function(dateObject, options) {
	return dojo.date.locale.format(dateObject, this.isoFormat);
      }
    }
  );

  // Setup the addition buttons.
  setupAdders();

}

dojo.addOnLoad(init);