dojo.require('dojo.NodeList-traverse');
dojo.require('dojo.data.ItemFileReadStore');
dojo.require('dojo.hash');
dojo.require('dijit.Tree');
dojo.require('dijit.tree.dndSource');
dojo.require('dijit.form.DateTextBox');
dojo.require('dojo.store.Memory');
dojo.require('dijit.form.FilteringSelect');

var mfUseful = {
  randomString: function() {
    var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
	  var string_length = 8;
	  var randomstring = '';
	  for (var i=0; i<string_length; i++) {
	    var rnum = Math.floor(Math.random() * chars.length);
		  randomstring += chars.substring(rnum,rnum+1);
	  }
    return randomstring;
  },
  hashToObject: function(hVal) {
    hVal = hVal || dojo.hash();
    
    var nhObj = dojo.queryToObject(hVal);
    
    if (typeof nhObj.displays !== 'undefined') {
      if (dojo.isArray(nhObj.displays) === false) {
        nhObj.displays = [ nhObj.displays ];
      }
      for (var mi = 0; mi < nhObj.displays.length; mi++) {
        // Convert from JSON.
        var mj = dojo.fromJson(decodeURI(nhObj.displays[mi]));
        // If the JSON string has escaped characters, sometimes it takes
        // more than one go to turn it into a JS object.
        while(dojo.isString(mj)) {
          mj = dojo.fromJson(mj);
        }
        nhObj.displays[mi] = mj;
      }
    }
    
    return nhObj;
  },
  objectToHash: function(nhObj) {
    if (typeof nhObj === 'undefined') {
      return null;
    }
    
    if (typeof nhObj.displays !== 'undefined') {
      for (var i = 0; i < nhObj.displays.length; i++) {
        nhObj.displays[i] = dojo.toJson(nhObj.displays[i]);
      }
    }
    
    return dojo.objectToQuery(nhObj);
  },
  onEnter: function(elementId, callback) {
    // Check that the element exists.
    if (dojo.byId(elementId) === null) {
	    return;
	  }
	
	  // Check that the callback function exists.
	  if (dojo.isFunction(callback) === false) {
	    return;
	  }
	
	  var handleKeys = function(evtObj) {
	    // Check for the Enter keys.
	    if (evtObj.keyCode === dojo.keys.ENTER ||
		      evtObj.keyCode === dojo.keys.NUMPAD_ENTER) {
			  // Fire the callback.
			  callback(evtObj);
		  }
	  };
	
	  // Connect the onkeydown event to the element.
	  dojo.connect(dojo.byId(elementId), 'onkeydown', handleKeys);
	
	  return;
  }
      
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
   * A function to call when the user has finished editing the box.
   * @type {function}
   */
  spec.editedFn = spec.editedFn || null;
  
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
            if (spec.editedFn !== null) {
              spec.editedFn(dojo.attr(spec.editableNode, 'id'), temp);
            }
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

  /**
   * Our randomly generated ID.
   * @type {string}
   */
  var theId = mfUseful.randomString();

  /**
   * The autocomplete memory.
   * @type {object}
   */
  var autocompleteMemory;

  /**
   * The filtering select element.
   * @type {object}
   */
  var filteringSelect;
  
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
   * Prepare an autocomplete memory.
   */
  var pointsMemory = function() {
    var pData = [];
    for (i = 0; i < allPoints.length; i++) {
      pData.push({
        name: allPoints[i],
        id: allPoints[i]
      });
    }
    autocompleteMemory = new dojo.store.Memory({
      data: pData
    });

    filteringSelect = new dijit.form.FilteringSelect({
      id: 'pointAutocomplete',
      name: 'pointAutocomplete',
      store: autocompleteMemory,
      searchAttr: 'name',
      queryExpr: '\.${0}*'
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
      id: theId
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
      
    dojo.publish('/monica/pointsSelected', [ clickedPoints ]);
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
    pointsMemory();
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
  
  /**
   * Remove our Dojo tree control.
   */
  that.removeTree = function() {
    dojo.destroy(theId);
  };

  /**
   * Attach a Dojo filtering select element to the named ID.
   * @param {string} nodeId The node to attach the element to.
   */
  that.attachFilter = function(nodeId) {
    if (typeof nodeId === 'undefined') {
      return;
    }
    
    dojo.byId(nodeId).appendChild(filteringSelect.domNode);
    
    // Activate it for use.
    mfUseful.onEnter(filteringSelect.id, compilePoints);
  };
  
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
   * The ID of our parent display.
   * @type {string}
   */
  spec.parentId = spec.parentId || '';
  
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
  
  // A function we call when our time-series options change.
  var optionsChangedFunc = spec.optionsChangedFunc || null;
  
  // Are we in the throes of loading.
  var isLoading = true;

  // Our methods follow.

  // Our private methods.
  /**
   * Watch out for a change to the hash.
   */
  var hashChanged = function(newHash) {
    // Get the hash in object format.
    var nhObj = mfUseful.hashToObject(newHash);
    var optionsUpdated = isLoading;
    
    // Check for our display.
    if (typeof nhObj.displays !== 'undefined') {
      for (var i = 0; i < nhObj.displays.length; i++) {
        if (nhObj.displays[i].id === spec.parentId) {
          // This is our display.
          // Check the time-series options.
          if (typeof nhObj.displays[i].options !== 'undefined') {
            // Have the options changed?
            if (nhObj.displays[i].options.spanTime !== 
                    seriesOptions.spanTime ||
                nhObj.displays[i].options.startTime !== 
                    seriesOptions.startTime ||
                nhObj.displays[i].options.maxPoints !== 
                    seriesOptions.maxPoints) {
              // The options have changed.
              for (var j = 0; j < ourSeries.length; j++) {
                if (ourSeries[j].monicaRef !== null &&
                    ourSeries[j].chartSeries !== null) {
                  // Remove each series from the plot.
                  removeSeriesFromPlot(ourSeries[j]);
                  // Ask for the series again with the new options.
                  // console.log(nhObj.displays[i].options.startTime);
                  ourSeries[j].monicaRef.
                    timeSeriesOptions(nhObj.displays[i].options);
                }
              }
              optionsUpdated = true;
            }
            // We alter our options.
            seriesOptions = dojo.clone(nhObj.displays[i].options);
          } else {
            // There are no options, so we put them in there.
            nhObj.displays[i].options = seriesOptions;
            dojo.hash(mfUseful.objectToHash(nhObj), true);
            optionsUpdated = true;
          }
          // Check the listed points.
          if (typeof nhObj.displays[i].points !== 'undefined') {
            for (var j = 0; j < nhObj.displays[i].points.length; j++) {
              addPoint(nhObj.displays[i].points[j]);
            }
          }
          if (optionsUpdated === true) {
            if (dojo.isFunction(optionsChangedFunc) === true) {
              optionsChangedFunc(that);
            }
          }
        }
      }
    }
  };

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

    // Send a message saying we need a MoniCA point.
    dojo.publish('/monica/pointRequired', {
      name: pointName,
      type: 'timeSeries',
      callbackFn: that.updatePlot,
      options: seriesOptions
    });
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
	      data: oRefs[i2Ci].monicaRef.getTimeSeries({
	        valueAsDecimalDegrees: true
        })
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
   * Remove a series from the plot.
   * @param {object} aSeries The series to remove.
   */
  var removeSeriesFromPlot = function(aSeries) {
    if (typeof aSeries === 'undefined') {
      return;
    }
    if (aSeries.chartSeries === null) {
      return;
    }
    aSeries.chartSeries.remove();
    aSeries.chartSeries = null;
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
  
  /**
   * Add a callback for when our series options are changed.
   * @param {function} callBack The function to call.
   */
  that.onOptionsChange = function(callBack) {
    optionsChangedFunc = callBack || optionsChangedFunc;
  };

  /**
   * Get the ID for our parent display.
   */
  that.parentId = function() {
    return spec.parentId;
  };
  
  hashChanged(dojo.hash());
  isLoading = false;
  dojo.subscribe('/dojo/hashchange', hashChanged);
  
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
   * The ID of our parent display.
   * @type {string}
   */
  spec.parentId = spec.parentId || '';

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
   * Watch out for a change to the hash.
   */
  var hashChanged = function(newHash) {
    // Get the hash in object format.
    var nhObj = mfUseful.hashToObject(newHash);
    
    // Check for our display.
    if (typeof nhObj.displays !== 'undefined') {
      for (var i = 0; i < nhObj.displays.length; i++) {
        if (nhObj.displays[i].id === spec.parentId) {
          // This is our display, so we check the listed points.
          if (typeof nhObj.displays[i].points !== 'undefined') {
            for (var j = 0; j < nhObj.displays[i].points.length; j++) {
              addPoint(nhObj.displays[i].points[j]);
            }
          }
        }
      }
    }
  };
  
  /**
   * Take a point name and turn it into an ID that can be assigned
   * to a DOM element.
   * @param {string} pointName The name of the point.
   */
  var makeSafeId = function(pointName) {
    return pointName.replace(/\./g, '_');
  };

  /**
   * Evaluate a point pattern for the table.
   * @param {string} pName The name of the point.
   */
  var producePattern = function(pName) {
    var rPattern;
    if (/^\D+\d*.*$/.test(pName) === true) {
      rPattern = /^(\D+)(\d*)(.*)$/i.exec(pName);
    } else if (/^\d+$/.test(pName) === true) {
      rPattern = [pName, 's', pName];
    }

    return rPattern;
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
    pointPattern = producePattern(pointElements[0]);
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
      tTable.pointPattern = producePattern(columnName);
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
    pointPattern = producePattern(pointElements[0]);
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
   * Modify our tables to remove a point.
   * @param {string} oldPoint The point name to remove.
   */
  var removePointFromTable = function(oldPoint) {
    // Which table?
    var aTable = whichTable(oldPoint);
    
    // Delete the table cell.
    if (aTable.table !== null &&
        aTable.column !== -1 &&
        aTable.row !== null) {
      dojo.destroy(aTable.row.columns[aTable.column]);
    }
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
    added = false;
    for (j = 0; j < ourPoints.length; j++) {
      if (ourPoints[j] === pointName) {
	      added = true;
	      break;
      }
    }

    // Add the point if it's not already there.
    if (added === false) {
      ourPoints.push(pointName);
    }

    // Update the table.
    addPoint2Table(pointName);

    // Send a message saying we need a MoniCA point.
    dojo.publish('/monica/pointRequired', {
      name: pointName,
      type: 'point',
      callbackFn: that.updateTable
    });
    
    return !added;
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
    // Check that we still have this ID.
    if (!dojo.byId(idPrefix)) {
      return;
    }
    pState = pointRef.latestValue();
    dojo.attr(idPrefix, 'innerHTML', pState.value);
    if (dojo.byId(idPrefix + 'Description')) {
      dojo.attr(idPrefix + 'Description', 'innerHTML', pDetails.description);
    }
    if (dojo.byId(idPrefix + 'Units')) {
      dojo.attr(idPrefix + 'Units', 'innerHTML', pDetails.units);
    }
    if (pState.errorState === true) {
      // This is an error condition.
      dojo.addClass(idPrefix, 'inError');
    } else {
      dojo.removeClass(idPrefix, 'inError');
    }
  };

  /**
   * Return the function that updates our table.
   */
  that.callbackFn = function() {
    return that.updateTable;
  };

  hashChanged(dojo.hash());
  dojo.subscribe('/dojo/hashchange', hashChanged);

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
  var myDisplays = { 
    active: null
  };

  // Our methods follow.

  // Our private methods.
  /**
   * Get a list of all the displays.
   */
  var allDisplays = function() {
    var retArr = [];
    for (i in myDisplays) {
      if (i !== 'active' &&
          myDisplays.hasOwnProperty(i)) {
        retArr.push(i);
      }
    }
    return retArr;
  };

  var checkTitle = function(displayObj) {
    // Check that we have our title right.
    if (typeof displayObj.title !== 'undefined') {
      dojo.attr(generateId(displayObj.id, 'editable'),
        'innerHTML', displayObj.title);
    }
  };
  
  /**
   * Watch out for a change to the hash.
   */
  var hashChanged = function(newHash) {
    // Get the hash in object format.
    var nhObj = mfUseful.hashToObject(newHash);
    // Get all our displays.
    var allIds = allDisplays();
    
    // Check for displays.
    if (typeof nhObj.displays !== 'undefined') {
      // Check that each display exists.
      for (i = 0; i < nhObj.displays.length; i++) {
        if (typeof myDisplays[nhObj.displays[i].id] === 'undefined') {
          // We need to create a new display.
          addDisplay(nhObj.displays[i].id);
          checkTitle(nhObj.displays[i]);
          if (nhObj.displays[i].type === 'pointTable') {
            makePointTable(nhObj.displays[i].id);
          } else if (nhObj.displays[i].type === 'timeSeries') {
            makeTimeSeries(nhObj.displays[i].id);
          }
        } else {
          checkTitle(nhObj.displays[i]);
        }
      }
    }
  };
  
  /**
   * Make a new display.
   */
  var addDisplay = function(nId) {
    newId = addContainer(nId);
    setupContainer(newId);
    activateDisplay(newId);
    return newId;
  };

  /**
   * Create and add a point table to the named display.
   */
  var makePointTable = function(nId) {
    var npt = pointTable({
      parentId: nId,
      tableDOM: generateId(nId, 'content')
    });
    myDisplays[nId].activeObject = npt;
  };
  
  /**
   * Create and add a time series to the named display.
   */
  var makeTimeSeries = function(nId) {
    // Add the time-series options.
    var aId = 'timeSeries' + nId + 'Option';
    var oId = generateId(myDisplays[nId].id, 'options');
    dojo.place(
      '<br /><label for="' + aId + 'MaxPoints">Display</label>' +
      '<input type="text" id="' + aId + 'MaxPoints" ' +
      'name="' + aId + 'MaxPoints" value="" size="5" />' +
      '<label for="' + aId + 'MaxPoints"> points covering</label>' +
      '<input type="text" id="' + aId + 'SpanTime" ' +
	    'name="' + aId + 'SpanTime" value="" size="5" />' +
      '<label for="' + aId + 'SpanTime"> minutes</label>' +
      '<select name="' + aId +
      'TimeType" id="' + aId + 'TimeType">' +
      '<option value="starting">starting</option>' +
      '<option value="ending">ending</option>' +
      '</select><label id="' + aId + 'NowLabel" ' +
      'for="' + aId + 'TimeType"> now</label>' +
      '<input type="text" id="' + aId + 'DateField" />' +
      '<input type="text" id="' + aId + 'TimeField" />' +
      '<button type="button" id="' + aId + 'OptionButton"' +
      'name="' + nId + '">Refresh</button>',
      oId, 'last'
    );

    // A function to show the correct time selection elements.
    var showOptions = function(sId, sType) {
      if (sType === 'ending') {
        dojo.removeClass(sId + 'NowLabel', 'elHide');
        dojo.addClass(sId + 'DateField', 'elHide');
        dojo.addClass(sId + 'TimeField', 'elHide');
      } else if (sType === 'starting') {
        dojo.addClass(sId + 'NowLabel', 'elHide');
        dojo.removeClass(sId + 'DateField', 'elHide');
        dojo.removeClass(sId + 'TimeField', 'elHide');
      }
    };
    
    // A function to fill in the time-series option boxes.
    var fillOptions = function(changedObj) {
      var cId = 'timeSeries' + changedObj.parentId() + 'Option';
      var cOptions = changedObj.getTimeSeriesOptions();
      dojo.attr(cId + 'SpanTime', 'value', cOptions.spanTime);
      if (cOptions.startTime === -1) {
        // Select the ending option.
        dojo.attr(cId + 'TimeType', 'value', 'ending');
        showOptions(cId, 'ending');
      } else {
        // Select the starting option.
        dojo.attr(cId + 'TimeType', 'value', 'starting');
        showOptions(cId, 'starting');
        // Fill in the time from the hash.
        var te = /^(.*?)\:(.*)$/.exec(cOptions.startTime);
        dojo.attr(cId + 'DateField', 'value', te[1]);
        dojo.attr(cId + 'TimeField', 'value', te[2]);
      }
      dojo.attr(cId + 'MaxPoints', 'value', cOptions.maxPoints);
    };

    var changeOptions = function(evtObj) {
      var cId = 'timeSeries' + evtObj.target.name + 'Option';
      var cOptions = {
        spanTime: dojo.attr(cId + 'SpanTime', 'value'),
        maxPoints: dojo.attr(cId + 'MaxPoints', 'value'),
        startTime: -1
      };
      // Set the start time if required.
      if (dojo.attr(cId + 'TimeType', 'value') === 'starting') {
        var pt = dojo.attr(cId + 'DateField', 'value') +
          ':' + dojo.attr(cId + 'TimeField', 'value');
        cOptions.startTime = pt;
      }
      // Get the current hash.
      var chObj = mfUseful.hashToObject(dojo.hash());
      // Find our display.
      for (var i = 0; i < chObj.displays.length; i++) {
        if (chObj.displays[i].id === evtObj.target.name) {
          chObj.displays[i].options = cOptions;
        }
      }
      dojo.hash(mfUseful.objectToHash(chObj));
    };
    
    dojo.connect(dojo.byId(aId + 'OptionButton'), 'onclick', changeOptions);

    var timeTypeChanged = function(evtObj) {
      // var cId = 'timeSeries' + evtObj.target.name + 'Option';
      var cId = evtObj.target.id.replace(/TimeType$/,'');
      // Get the option that is now selected.
      var selValue = dojo.attr(cId + 'TimeType', 'value');
      showOptions(cId, selValue);
      if (selValue === 'starting') {
        // Check for values in the starting boxes.
        var dValue = dojo.attr(cId + 'DateField', 'value');
        var tValue = dojo.attr(cId + 'TimeField', 'value');
        if (dValue === '' || tValue === '') {
          // We need to fill in some values, using the current time.
          var tTime = new Date();
          var td = tTime.getUTCFullYear() + '-' +
            (tTime.getUTCMonth() + 1).zeroPad(10) + '-' +
            tTime.getUTCDate().zeroPad(10);
          var tt = tTime.getUTCHours().zeroPad(10) + ':' +
            tTime.getUTCMinutes().zeroPad(10) + ':' +
            tTime.getUTCSeconds().zeroPad(10);
          dojo.attr(cId + 'DateField', 'value', td);
          dojo.attr(cId + 'TimeField', 'value', tt);
        }
      }
    };
    dojo.connect(dojo.byId(aId + 'TimeType'), 'onchange', timeTypeChanged);
    
    var npt = timeSeries({
      parentId: nId,
      plotDOM: generateId(nId, 'content'),
      plottingLibrary: 'highcharts',
      optionsChangedFunc: fillOptions
    });
    myDisplays[nId].activeObject = npt;
    
  };
  
  /**
   * Add a container for the display.
   */
  var addContainer = function(nId) {
    // Make a new ID.
    idString = nId || mfUseful.randomString();

    // Make the div.
    newDiv = dojo.create('div',
      {
	      id: idString,
	      'class': spec.containerClass
      }
    );
    // And push it on to our displays list.
    myDisplays[idString] = {
      id: idString,
      node: newDiv,
      activeObject: null
    };

    // Add it to the page.
    dojo.place(newDiv, spec.referenceNode);

    // Return the ID of this new node.
    return idString;
  };

  /**
   * Change the text of our display title.
   * @param {string} editableId The ID of the editable object.
   * @param {string} newTitle The text of the new title.
   */
  var getNewText = function(editableId, newTitle) {
    // Get the hash in object format.
    var nhObj = mfUseful.hashToObject();
    for (i = 0; i < nhObj.displays.length; i++) {
      var tId = generateId(nhObj.displays[i].id, 'editable');
      if (tId === editableId) {
        nhObj.displays[i].title = newTitle;
      }
    }
    dojo.hash(mfUseful.objectToHash(nhObj));
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
      editableNode: editableSpan,
      editedFn: getNewText
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
    if (myDisplays['active'] === null) {
      return;
    }
    var ca = myDisplays['active'].id;
    if (ca !== '') {
      dojo.removeClass(generateId(ca, 'active'),
		    'containerActive');
      dojo.addClass(generateId(ca, 'active'),
		    'containerInactive');
    }
    myDisplays['active'] = null;
  };

  /**
   * Activate the named display.
   * @param {string} container The ID of the container to activate.
   */
  var activateDisplay = function(container) {
    if (myDisplays['active'] != null &&
        myDisplays['active'].id === container) {
      return;
    }
    deactivateCurrent();
    myDisplays['active'] = myDisplays[container];
    if (myDisplays['active'] != null) {
      activeId = generateId(myDisplays['active'].id, 'active');
      dojo.removeClass(activeId, 'containerInactive');
      dojo.addClass(activeId, 'containerActive');
    }
  };

  // Our public methods.
  /**
   * Add a new display.
   */
  that.add = function(nId) {
    return addDisplay(nId);
  };

  /**
   * Return the title text for a named display.
   * @param {string} cId The ID of the display to query.
   */
  that.displayTitle = function(cId) {
    return dojo.attr(generateId(cId, 'editable'), 'innerHTML');
  };
  
  /**
   * Check for a particular display ID.
   * @param {string} cId The ID of the display to check for.
   */
  that.hasId = function(cId) {
    if (typeof myDisplays[cId] !== 'undefined') {
      return true;
    }
    return false;
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
      assignId = myDisplays['active'].id;
    }
    myDisplays[assignId].activeObject = assignObject;
  };

  /**
   * Return the object for the currently active display.
   */
  that.activeObject = function() {
    return myDisplays['active'].activeObject;
  };

  /**
   * Return the object for the named display.
   * @param {string} oId The ID to retrieve the object for.
   */
  that.getObject = function(oId) {
    if (typeof myDisplays[oId] !== 'undefined') {
      return myDisplays[oId].activeObject;
    }
    return null;
  };
  
  /**
   * Return the ID of the currently active display.
   */
  that.activeId = function() {
    return myDisplays['active'].id;
  };
  
  /**
   * Return the DOM ID of the content node for the active display.
   */
  that.activeContent = function() {
    return generateId(myDisplays['active'].id, 'content');
  };

  /**
   * Return the DOM ID of the options node for the active display.
   */
  that.activeOptions = function() {
    return generateId(myDisplays['active'].id, 'options');
  };

  /**
   * Get a list of all the displays.
   */
  that.allDisplays = function() {
    return allDisplays();
  };

  // Go through the hash on initialisation.
  hashChanged(dojo.hash());
  dojo.subscribe('/dojo/hashchange', hashChanged);
  
  return that;
};


/**
 * This is the function called on page load.
 */
var init = function() {
  // Here is the list of our MoniCA servers.
  var monicaServers = [
    {
      serverName: 'monhost-nar',
      location: 'ATCA'
    },
    {
      serverName: 'monhost-pks',
      location: 'Parkes'
    },
    {
      serverName: 'monhost-mop',
      location: 'Mopra'
    }
  ];

  // Fill the server list.
  for (var i = 0; i < monicaServers.length; i++) {
    var servero = dojo.create('option', {
      innerHTML: monicaServers[i].location,
      value: monicaServers[i].serverName
    });
    dojo.place(servero, 'serverselect');
  }

  // Set up a default MoniCA object.
  var setupOptions = {
    updateInterval: 2000,
    autoDescriptions: true
  };

  // The reference to the MoniCA server object.
  var monica = null;
  // And an indicator to say we've connected.
  var monicaConnected = false;

  // A list of points that are requested before MoniCA
  // has connected.
  var prePoints = [];
  
  // Our tree.
  var treeDisplay;

  /**
   * This routine is called when the hash has been changed and Dojo has
   * published an event to the '/dojo/hashchange' channel. It looks at
   * the new hash and calls the appropriate routines to keep the page
   * consistent.
   * @param {string} newHash The new value of the location hash.
   */
  var hashChanged = function(newHash) {
    // The hash has changed, so we do things.
    
    // Convert the new hash into an object.
    var nhObj = mfUseful.hashToObject(newHash);
    
    // Check if the new hash has a server entry.
    if (typeof nhObj.serverName === 'undefined') {
      // We have no connection so we disconnect.
      disconnectServer();
    } else {
      // We have a connection so we connect.
      connectServer(nhObj.serverName);
    }
  };
  
  /**
   * This routine is called when the server connect button is pressed and
   * inserts the location value into the page hash, or removes it if the
   * user wants to disconnect it.
   */
  var connectButtonActions = function() {
    if (monica === null) {
      // Which server is selected?
      var selectedOptionIndex = dojo.byId('serverselect').selectedIndex;
      var currPageConfig = {
        serverName:
          dojo.attr(dojo.byId('serverselect').options[selectedOptionIndex],
                  'innerHTML')
      };
    } else {
      var currPageConfig = {};
    }
    
    dojo.hash(mfUseful.objectToHash(currPageConfig));
  };

  /**
   * This routine is called when the "Add Point Table" button is pressed.
   * It adds enough information to the page hash to get a point table
   * display to show up.
   */
  var addPointTable = function() {
    // Make a new random ID for the display.
    var newId = mfUseful.randomString();
    var nDis = {
      id: newId,
      type: 'pointTable'
    };
    var ndJ = dojo.toJson(nDis);
    
    // Get the current location hash.
    var dhObj = mfUseful.hashToObject();
    if (typeof dhObj.displays === 'undefined') {
      dhObj.displays = [ ndJ ];
    } else {
      dhObj.displays.push(ndJ);
    }
    
    dojo.hash(mfUseful.objectToHash(dhObj));
    
  };

  /**
   * This routine is called when the "Add Time Series" button is pressed.
   * It adds enough information to the page hash to get a time series
   * display to show up.
   */
  var addTimeSeries = function() {
    var newId = mfUseful.randomString();
    var nDis = {
      id: newId,
      type: 'timeSeries'
    };
    var ndJ = dojo.toJson(nDis);

    // Get the current location hash.
    var dhObj = mfUseful.hashToObject();
    if (typeof dhObj.displays === 'undefined') {
      dhObj.displays = [ ndJ ];
    } else {
      dhObj.displays.push(ndJ);
    }
    
    dojo.hash(mfUseful.objectToHash(dhObj));

  };

  /**
   * This routine is called when some points are selected in the tree and
   * the pointsSelected channel is published to. It adds a list of the
   * points to the right location in the hash.
   * @param {array} pointsList The list of points the user selected.
   */
  var pointsSelected = function(pointsList) {
    // Get the active display's ID.
    var activeId = displays.activeId();
    var currPageConfig = mfUseful.hashToObject();
    // Find this in our current config.
    for (var i = 0; i < currPageConfig.displays.length; i++) {
      if (currPageConfig.displays[i].id === activeId) {
        if (typeof currPageConfig.displays[i].points === 'undefined') {
          currPageConfig.displays[i].points = [];
        }
        for (var j = 0; j < pointsList.length; j++) {
          currPageConfig.displays[i].points.push(pointsList[j]);
        }
        break;
      }
    }
    
    dojo.hash(mfUseful.objectToHash(currPageConfig));
  };

  /**
   * This routine is called when the hash has indicated that we need to
   * connect to a MoniCA server.
   * @param {string} locationName The name of the location to connect to.
   */
  var connectServer = function(locationName) {
    // Only connect if we need to.
    if (monica !== null) {
      return;
    }
    
    // Find the server in our list.
    var selectOptions = dojo.byId('serverselect').options;
    for (var i = 0; i < selectOptions.length; i++) {
      if (dojo.attr(selectOptions[i], 'innerHTML') === locationName) {
        selectOptions[i].selected = true;
        setupOptions.serverName = selectOptions[i].value;
        monica = monicaServer(setupOptions);
        monica.connect().then(pointsKnown);
      }
    }
    configurePageState();
  };
  
  /**
   * This routine is called when the hash has indicated that we need to
   * disconnect from a MoniCA server.
   */
  var disconnectServer = function() {
    if (monica === null) {
      // We don't have a connection.
      return;
    }
    
    treeDisplay.removeTree();
    monica = null;
    configurePageState();
  };

  /**
   * This routine detects whether we are connected to a MoniCA server
   * or not, and configures elements in the page accordingly.
   */
  var configurePageState = function() {
    if (monica === null) {
      // The button will make a connection.
      dojo.attr('serverselectConnect', 'innerHTML', 'Connect');
      // The user can change the server in the select.
      dojo.byId('serverselect').disabled = false;
      // The user can't add elements to the page.
      dojo.addClass('topadder', 'elHide');
      dojo.addClass('bottomadder', 'elHide');
    } else {
      // The button will end a connection.
      dojo.attr('serverselectConnect', 'innerHTML', 'Disconnect');
      // The user can't change the server in the select.
      dojo.byId('serverselect').disabled = true;
      // The user can add elements to the page.
      dojo.removeClass('topadder', 'elHide');
      dojo.removeClass('bottomadder', 'elHide');
    }
  };

  /**
   * This routine is called when a MoniCA connection has been
   * successfully established and is responsible for setting up
   * the points tree.
   * @param {object} serverInfo Information about the server that has
   *                            just connected.
   */
  var pointsKnown = function(serverInfo) {
    // We make a point tree a put it on the page so the user
    // can select which points to put into the view objects.
    var allPoints = monica.pointsList();
    
    // Make a Dojo tree control and attach it to the page.
    treeDisplay = pointSelector();
    treeDisplay.setPoints(allPoints);
    treeDisplay.attachTree('treearea');
    // treeDisplay.attachFilter('searcharea');
    
    monicaConnected = true;
    monica.startUpdating();
    pointStart();
  };

  /**
   * This routine is called when a page element requires a point to
   * start updating.
   * @param {object} pName Object describing the point to add to MoniCA.
   */
  var pointStart = function(pName) {
    if (monicaConnected === false) {
      // MoniCA isn't connected so we add it to a list of points
      // that will get added when the connection is established.
      prePoints.push(pName);
      return;
    } else if (monicaConnected === true && prePoints.length > 0 &&
               typeof pName === 'undefined') {
      // We add all the pre-defined points.
      for (var i = 0; i < prePoints.length; i++) {
        pointAdder(prePoints[i]);
      }
    } else {
      // We are being called after MoniCA has connected.
      pointAdder(pName);
    }
    // monica.getDescriptions();
  };
  
  /**
   * This routine is called when we are ready to add a point to
   * the MoniCA querier to begin it updating.
   * @param {object} pDetails Object describing the point to add to MoniCA.
   */
  var pointAdder = function(pDetails) {
    if (typeof pDetails === 'undefined') {
      return;
    }
    
    if (pDetails.type === 'point') {
      // A simple point.
      var pRef = monica.addPoints([pDetails.name]);
      pRef[0].addCallback(pDetails.callbackFn);
    } else if (pDetails.type === 'timeSeries') {
      // A time series.
      var tRef = monica.addTimeSeries({
        pointName: pDetails.name,
        timeSeriesOptions: pDetails.options
      });
      tRef.addCallback(pDetails.callbackFn);
    }
  };
  
  /**
   * This routine connects subscription channels with our routines to
   * deal with them.
   */
  var setupSubscriptions = function() {
    dojo.subscribe('/dojo/hashchange', hashChanged);
    
    dojo.subscribe('/monica/pointsSelected', pointsSelected);
    
    dojo.subscribe('/monica/pointRequired', pointStart);
  };

  /**
   * This routine connects the existing HTML buttons to the appropriate
   * routines.
   */
  var setupAdders = function() {
    // Make the buttons do things.
    dojo.connect(dojo.byId('topadderPointTable'), 'onclick',
      addPointTable);
    dojo.connect(dojo.byId('bottomadderPointTable'), 'onclick',
      addPointTable);

    dojo.connect(dojo.byId('topadderTimeSeries'), 'onclick',
      addTimeSeries);
    dojo.connect(dojo.byId('bottomadderTimeSeries'), 'onclick',
      addTimeSeries);
  };

  // Setup the addition buttons and topic subscriptions.
  setupAdders();
  setupSubscriptions();

  // Get a new display handler.
  var displays = displayHandler({
    referenceNode: 'elementsarea'
  });

  // Make the connect button do something.
  dojo.connect(dojo.byId('serverselectConnect'), 'onclick',
    connectButtonActions);
  configurePageState();

  hashChanged();
};

/**
 * Convert a number into a string with leading zeroes if required.
 * @param {number} bound The largest number (+1) to pad with leading zeroes.
 */
Number.prototype.zeroPad = function(bound) {
	var b = bound || 1;
	var o = '';
	var n = this;
	
	var sign = 1;
	if (n < 0) {
		sign = -1;
		n *= sign;
	}
	
	while (n < b && b >= 10) {
		o += '0';
		b /= 10;
	}

	if (sign === -1) {
		o = '-' + o;
	}
	
	return o + n;
};


dojo.addOnLoad(init);