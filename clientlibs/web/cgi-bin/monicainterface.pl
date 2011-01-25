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
	print $point_vals[$i]->point." ".bat2cal($point_vals[$i]->bat,0)." ".$point_vals[$i]->val.
	    " ".$point_vals[$i]->errorstate."\n";
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
	print $point_descriptions[$i]->point."|".$point_descriptions[$i]->updatetime."|".
	    $point_descriptions[$i]->units."|".$point_descriptions[$i]->description."\n";
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
#	    print "$current_mjd is current MJD\n";
 	    # take away the interval
 	    my $start_mjd=$current_mjd-$interval/(60.0*24.0); # taking away fraction of a day
# 	    print "$start_mjd is starting MJD\n";
	    
#	    print mjd2bat($start_mjd)->as_hex." is starting BAT\n";
#	    print bat2cal(mjd2bat($start_mjd)->as_hex)." is starting time\n";
#	    exit;
 	    # get the data
 	    my @point_timevals=monsince_new($mon,$start_mjd,$pointname,$maxnper);
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
	    my @start_time=($tels[5],$tels[4],$tels[3],$tels[2],$tels[1]-1,$tels[0]-1900);
#	    for (my $j=0;$j<=$#start_time;$j++){
#		print $start_time[$j]." ";
#	    }
#	    print "\n";
	    # convert it to MJD
	    my $start_mjd=perltime2mjd(@start_time);
# 	    print "$start_mjd is starting MJD\n";
#	    print bat2cal(mjd2bat($start_mjd)->as_hex)." is starting time\n";
	    # add the interval
	    my $end_mjd=$start_mjd+$interval/(60.0*24.0); # adding fractions of a day
# 	    print "$end_mjd is starting MJD\n";
#	    print bat2cal(mjd2bat($end_mjd)->as_hex)." is ending time\n";

	    # get the data
	    my @point_timevals=monbetween_new($mon,$start_mjd,$end_mjd,$pointname,$maxnper);
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

sub perltime2mjd {
    # perl time should be (second,minute,hour,day,month-1,year-1900)
    my @fulltime=@_;
    
    my $ut=hms2time($fulltime[2],$fulltime[1],$fulltime[0]);
    my $mjd=cal2mjd($fulltime[3],$fulltime[4]+1,$fulltime[5]+1900,$ut);

    return $mjd;
}    


sub bat2unixtime($;$) {
    my $bat=Math::BigInt->new(shift);

    my $dUT=shift;
    $dUT=0 if (!defined $dUT);

    my $mjd=(Math::BigFloat->new($bat)/1e6-$dUT)/60/60/24;
    
    my ($day,$month,$year,$ut)=mjd2cal($mjd->bstr());

    $ut*=24.0;
    my $hour=floor($ut);
    $ut-=$hour;
    $ut*=60.0;
    my $minute=floor($ut);
    $ut-=$minute;
    $ut*=60.0;
    my $second=floor($ut);
    
    $year-=1900;
    $month-=1;
    my $utime=timegm($second,$minute,$hour,$day,$month,$year,0,0);
    
    return $utime;

}

sub bat2cal($;$) {
    my $bat=Math::BigInt->new(shift);

    my $dUT=shift;
    $dUT=0 if (!defined $dUT);

    my $mjd=(Math::BigFloat->new($bat)/1e6-$dUT)/60/60/24;
#    print "bat2cal has MJD = ".$mjd."\n";
    
    my ($day,$month,$year,$ut)=mjd2cal($mjd->bstr());
#    print "cal = $day $month $year $ut\n";

    $ut*=24.0;
    my $hour=floor($ut);
    $ut-=$hour;
    $ut*=60.0;
    my $minute=floor($ut);
    $ut-=$minute;
    $ut*=60.0;
    my $second=floor($ut);

    my $caltime=sprintf "%04d-%02d-%02d_%02d:%02d:%02d",$year,$month,$day,$hour,$minute,$second;

    return $caltime;
}

sub monnames ($) {
    # a function that Chris apparently forgot, this will return all the
    # available monitoring points
    my ($mon)=@_;

    print $mon "names\n";
    
    my @names;
    
    my $num_names=<$mon>; # the number of names being returned
    for (my $i=0;$i<$num_names;$i++){
	chomp(my $line=<$mon>);
	push @names,$line;
    }

    return @names;
}

sub mondetails ($@) {
    my $mon=shift;
    my @monpoints=@_;
    my $npoll=scalar(@monpoints);

    if ($npoll==0){
	warn "No monitor points requested!\n";
	return undef;
    }

    print $mon "details\n";
    print $mon "$npoll\n";
    foreach (@monpoints) {
	print $mon "$_\n";
    }

    my @vals=();
    
    for (my $i=0;$i<$npoll;$i++){
	my $line=<$mon>;
	push @vals,new MonDetail($line);
    }

    if (wantarray){
	return @vals;
    } else {
	return $vals[0];
    }
    
}

sub monbetween_new ($$$$;$) {
#    my ($mon,$mjd1,$mjd2,$point) = @_;
    my $mon=shift;
    my $mjd1=shift;
    my $mjd2=shift;
    my $point=shift;
    my $maxnper=shift;
    $maxnper=-1 if (!defined $maxnper);

    my $bat1=mjd2bat($mjd1)->as_hex;
    my $bat2=mjd2bat($mjd2)->as_hex;

    my $nreceived;
    my @vals=();
    my $maxbat=bat2mjd($bat1);
    do {
	print $mon "between\n";
#	print "getting between ".mjd2bat($maxbat)->as_hex." $bat2\n";
	print $mon mjd2bat($maxbat)->as_hex." $bat2 $point\n";

	my $nreceived=<$mon>;
#	print "receiving $nreceived points\n";
	my $acceptfraction=ceil($nreceived/$maxnper);
	my $j=0;
	for (my $i=0;$i<$nreceived;$i++){
	    my $line=<$mon>;
	    $j++;
	    if (($acceptfraction<0)||($j>=$acceptfraction)||
		($i==0)||($i==($nreceived-1))){
		$j=0;
		push @vals,new MonBetweenPoint($line);
		if (bat2mjd($vals[$#vals]->bat)>$maxbat){
		    $maxbat=bat2mjd($vals[$#vals]->bat);
		}
	    }
	}
	# increment the max bat by 1 millisecond
	$maxbat+=(1e-3/60/60/24);
	# check whether we've gone past the last bat
	if ($maxbat>=bat2mjd($bat2)){
	    last;
	}
    } while ($nreceived>1);

    return @vals;
}

sub monsince_new ($$$;$) {
#    my ($mon,$mjd,$point) = @_;
    my $mon=shift;
    my $mjd=shift;
    my $point=shift;
    my $maxnper=shift;
    $maxnper=-1 if (!defined $maxnper);
    
    my $bat = mjd2bat($mjd)->as_hex;
#    print "transferred bat = ".$bat."\n";
    
    my $nreceived;
    my @vals=();
    my $maxbat=bat2mjd($bat);
    do {
	print $mon "since\n";
#	print "getting from ".mjd2bat($maxbat)->as_hex."\n";
	print $mon mjd2bat($maxbat)->as_hex." $point\n";
	
	$nreceived=<$mon>;
#	print "receiving $nreceived points\n";
	my $acceptfraction=ceil($nreceived/$maxnper);
	my $j=0;
	for (my $i=0;$i<$nreceived;$i++){
	    my $line=<$mon>;
	    $j++;
	    if (($acceptfraction<0)||($j>=$acceptfraction)||
		($i==0)||($i==($nreceived-1))){
		$j=0;
		push @vals,new MonSincePoint($line);
		if (bat2mjd($vals[$#vals]->bat)>$maxbat){
		    $maxbat=bat2mjd($vals[$#vals]->bat);
		}
	    }
	}
	# increment the max bat by 1 millisecond
	$maxbat+=(1e-3/60/60/24);
    } while ($nreceived>1);

    return @vals;
}

sub monpoll2 ($@) {
    my $mon=shift;
    my @monpoints=@_;
    my $npoll=scalar(@monpoints);

    if ($npoll==0){
	warn "No monitor points requested!\n";
	return undef;
    }

    print $mon "poll2\n";
    print $mon "$npoll\n";
    foreach (@monpoints) {
	print $mon "$_\n";
    }

    my @vals=();

    for (my $i=0;$i<$npoll;$i++){
	my $line=<$mon>;
	push @vals,new MonFullPoint($line);
    }

    if (wantarray){
	return @vals;
    } else {
	return $vals[0];
    }
}


package MonDetail;

sub new {
    my $proto=shift;
    my $class=ref($proto) || $proto;

    my $monline=shift;
    my $self=[$monline=~/^(\S+)\s+(\S+)\s+\"(.*?)\"\s+\"(.*)\"$/];

    bless ($self,$class);
}

sub point {
    my $self=shift;
    if (@_) { $self->[0] = shift }
    return $self->[0];
}

sub updatetime {
    my $self=shift;
    if (@_) { $self->[1] = shift }
    return $self->[1];
}

sub units {
    my $self=shift;
    if (@_) { $self->[2] = shift }
    return $self->[2];
}

sub description {
    my $self=shift;
    if (@_) { $self->[3] = shift }
    return $self->[3];
}

package MonFullPoint;

sub new {
    my $proto=shift;
    my $class=ref($proto)||$proto;
    
    my $monline=shift;
    my $self=[$monline=~/^(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)$/];
    
    bless ($self,$class);
}

sub point {
    my $self=shift;
    if (@_) { $self->[0] = shift }
    return $self->[0];
}

sub bat {
    my $self=shift;
    if (@_) { $self->[1] = shift }
    return $self->[1];
}

sub val {
    my $self=shift;
    if (@_) { $self->[2] = shift }
    return $self->[2];
}

sub units {
    my $self=shift;
    if (@_) { $self->[3] = shift }
    return $self->[3];
}

sub errorstate {
    my $self=shift;
    if (@_) { $self->[4] = shift }
    return $self->[4];
}

package MonSincePoint;

sub new {
    my $proto=shift;
    my $class=ref($proto)||$proto;

    my $monline=shift;
    my $self=[$monline=~/^(\S+)\s+(\S+)$/];
    
    bless ($self,$class);
}

sub bat {
    my $self=shift;
    if (@_) { $self->[0] = shift }
    return $self->[0];
}

sub val {
    my $self=shift;
    if (@_) { $self->[1] = shift }
    return $self->[1];
}
    
