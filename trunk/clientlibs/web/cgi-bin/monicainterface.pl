#!/usr/bin/perl

use strict;
use CGI;
use ATNF::MoniCA;
use Time::Local;
use Astro::Time;
use POSIX;

my $in=CGI->new;

my %input=$in->Vars;

# start the HTML output
print $in->header('text/html');

#debugging
#$input{"server"}="monhost-nar";

# the input should contain the name of the server to connect to
my $mon=monconnect($input{"server"});

if (!defined $mon){
    print "Could not connect to monitor host server ".$input{"server"};
    exit;
}

# do something now
my $action=$input{"action"};

# debugging
#$action="intervals";
#$input{"points"}="site.environment.weather.BoxTemperature,2011-01-11:23:37:29,14400,200;site.environment.weather.BoxHumidity,-1,60,200".
#    ";site.environment.weather.DewPoint,2011-01-01:00:01:00,5,200";
#$input{"points"}="site.environment.weather.BoxHumidity;site.environment.weather.BoxTemperature;site.environment.weather.DewPoint;site.environment.weather.PrecipitableWater";

if ($action eq "points"){
    # get a number of points
    # the calling routine should have sent us a string called "points"
    # that is a semi-colon separated list of the points they want
    my @required_points=split(/\;/,$input{"points"});

    # get the points
    my @point_vals=monpoll2($mon,@required_points);

    # print back the points
    for (my $i=0;$i<=$#point_vals;$i++){
	print $point_vals[$i]->point." ".bat2cal($point_vals[$i]->bat,0)." ".
	    $point_vals[$i]->val." ".$point_vals[$i]->errorstate."\n";
    }

} elsif ($action eq "names"){
    # get a list of all the available monitoring points
    my @monitoring_points=monnames($mon);

    # print them all
    for (my $i=0;$i<=$#monitoring_points;$i++){
	print $monitoring_points[$i]."\n";
    }

} elsif ($action eq "descriptions"){
    # get the details for the supplied monitoring points
    my @required_points=split(/\;/,$input{"points"});
    
    # get the descriptions
    my @point_descriptions=mondetails($mon,@required_points);

    # print back the descriptions
    for (my $i=0;$i<=$#point_descriptions;$i++){
	print $point_descriptions[$i]->point."|".
	    $point_descriptions[$i]->updatetime."|".
	    $point_descriptions[$i]->units."|".
	    $point_descriptions[$i]->description."\n";
    }
} elsif ($action eq "intervals"){
    # get info for all the supplied points between the specified points
    
    # split the points up
    my @point_infos=split(/\;/,$input{"points"});
 
    # each point will be "pointname,starttime,interval"
    # starttime is calendar time (yyyy/mm/dd:HH:MM:SS)
    # interval is in minutes
    for (my $i=0;$i<=$#point_infos;$i++){
 	my ($pointname,$starttime,$interval,$maxnper)=split(/\,/,$point_infos[$i]);
 	if ($starttime==-1){
 	    # no start time, we just want the latest data
 	    # need to know the current time
 	    my @current_time=gmtime(time);
 	    # get that as MJD
 	    my $current_mjd=perltime2mjd(@current_time);
 	    # take away the interval
 	    my $start_mjd=$current_mjd-$interval/(60.0*24.0);
	    
 	    # get the data
 	    my @point_timevals=monsince($mon,$start_mjd,$pointname,$maxnper);
	    # print back the data as JSON
	    print "{ name: '".$pointname."', data: [";
	    for (my $j=0;$j<=$#point_timevals;$j++){
		if ($j>0){
		    print ",";
		}
		if ($point_timevals[$j]){
		    my $tval=$point_timevals[$j]->val;
		    $tval=~s/[\'\x{00b0}]/\:/g;
		    $tval=~s/\"//g;
		    print "[".(bat2unixtime($point_timevals[$j]->bat)*1000).",";
		    if ($tval=~/\:/){
			print "\"".$tval."\"";
		    } else {
			print $tval;
		    }
		    print "]";
		}
	    }
	    print "]}\n";
 	} else {
	    # we have a start time, we need to parse it
	    my @tels=split(/[\:\-]/,$starttime);
	    my @start_time=($tels[5],$tels[4],$tels[3],$tels[2],$tels[1]-1,
			    $tels[0]-1900);
	    # convert it to MJD
	    my $start_mjd=perltime2mjd(@start_time);
	    # add the interval
	    my $end_mjd=$start_mjd+$interval/(60.0*24.0); # adding fractions of a day

	    # get the data
	    my @point_timevals=monbetween_new($mon,$start_mjd,$end_mjd,$pointname,
					      $maxnper);
	    # print back the data as JSON
	    print "{ name: '".$pointname."', data: [";
	    for (my $j=0;$j<=$#point_timevals;$j++){
		if ($j>0){
		    print ",";
		}
		if ($point_timevals[$j]){
		    my $tval=$point_timevals[$j]->val;
		    $tval=~s/[\'\x{00b0}]/\:/g;
		    $tval=~s/\"//g;
		    print "[".(bat2unixtime($point_timevals[$j]->bat)*1000).",";
		    if ($tval=~/\:/){
			print "\"".$tval."\"";
		    } else {
			print $tval;
		    }
		    print "]";
		}
	    }
	    print "]}\n";
	}
 	    
    }
}

# finished
exit;
