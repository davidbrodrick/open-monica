module atnf {
  module atoms {
    module mon {
      module comms {
        ////////////
        //Data structure definitions
        sequence<string> stringarray;

        //PointDescriptionIce contains pickled fields which fully describe
        //a specific point
        struct PointDescriptionIce {
          stringarray names;
          string      source;
          string      description;
          string      shortdescription;
          string      units;
          bool        enabled;
          stringarray inputtransactions;
          stringarray outputtransactions;
          stringarray translations;
          stringarray limits;
          stringarray archivepolicies;
          long        period;
          int         archivelongevity;
        };
        sequence<PointDescriptionIce> pointarray;

        //Enum to represent data type        
        enum DataType {DTNull, DTFloat, DTDouble, DTInt, DTLong, DTString, DTBoolean, DTAbsTime, DTRelTime, DTAngle};
        //Classes to hold different data types
        class DataValue { 
          DataType type;
        };
        class DataValueFloat extends DataValue {
          float value;
        };        
        class DataValueDouble extends DataValue {
          double value;
        };
        class DataValueInt extends DataValue {
          int value;
        };        
        class DataValueLong extends DataValue {
          long value;
        };
        class DataValueString extends DataValue {
          string value;
        };
        class DataValueBoolean extends DataValue {
          bool value;
        };
        class DataValueRelTime extends DataValue {
          long value;
        };
        class DataValueAbsTime extends DataValue {
          long value;
        };
        class DataValueAngle extends DataValue {
          double value;
        };
        
        //PointDataIce encapsulates a single record/datum
        struct PointDataIce {
          string    name;
          long      timestamp;
          DataValue value;
          bool      alarm;
        };
        sequence<PointDataIce> pointdataset;
        sequence<pointdataset> pointdatasetarray;
        
        
        
        ////////////
        //The main interface between clients and the server
        interface MoniCAIce {
          ////////////
          //Operations for getting/setting point metadata
          //
          //Return the names of all points on the system
          idempotent stringarray getAllPointNames();
          //Return full details for the specified points
          //Result array may be shorter than the request array if some points were not found
          idempotent pointarray getPoints(stringarray names);
          //Return full details for all points on the system
          idempotent pointarray getAllPoints();
          //Add/update the definitions for the specified points
          bool addPoints(pointarray newpoints, string username, string passwd);
          
          ////////////
          //Operations relating to getting/setting data
          //
          //Return historical data for the given points.
          //Set maxsamples to zero to impose no limit.
          //Server may impose a limit on the size of the return structure, so the client needs
          //to use a loop, advancing the start epoch, until all require data has been collected.
          //Note that the name field within the PointDataIce objects will be set to blank by the
          //server to minimise network bandwidth. The client can reinsert this name in each datum
          //if required by matching it with the name of the requested point.
          idempotent pointdatasetarray getArchiveData(stringarray names, long start, long end, long maxsamples);
          //Get latest data for the given points
          idempotent pointdataset getData(stringarray names);
          //Set new values for the given points
          //If israw is true then translation will be applied to the specified values
          bool setData(stringarray names, pointdataset values, string username, string passwd);

          ////////////
          //Operations relating to 'SavedSetups'. These are basically pickled
          //dictionaries which are currently used to contain predefined page
          //layouts for the GUI client (but which may have other uses in the 
          //future).
          //
          //Get the list of all saved page setups from the server
          idempotent stringarray getAllSetups();
          //Add/update a new setup to the server's list
          bool addSetup(string setup, string username, string passwd);
          
          ////////////
          //Some miscellaneous operations
          //
          //Obtain public key and modulus to use for authenticated operations
          idempotent stringarray getEncryptionInfo();
          //Get current time from server
          idempotent long getCurrentTime();
        };
        
        
        //Structure for subscribing to updates via an IceStorm topic
        struct PubSubRequest {
          string topicname;
          stringarray pointnames;
        };
        
        interface PubSubControl {
          ////////////
          //Subscribe to updates via an IceStorm topic.
          void subscribe(PubSubRequest req);
          
          ////////////
          //Cancel the subscriptions through the given topic
          void unsubscribe(string topicname);
          
          ////////////
          //Notify the server that the specified topic is still active.
          void keepalive(string topicname);
        };
        
        interface PubSubClient {
          ////////////
          //Receive new updates for one or more points
          void updateData(pointdataset newdata);
        };
      };
    };
  };
};
