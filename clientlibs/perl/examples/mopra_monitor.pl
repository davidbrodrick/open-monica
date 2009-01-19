#!/usr/local/bin/perl -w

use strict;

use lib '/nfs/wwwatcgi/vlbi/perllib';

use CGI::Pretty qw/:standard *table *Tr/;

use Astro::Time;
use ATNF::MoniCA;

my $server = 'monhost-mop.atnf.csiro.au';

my $mon = monconnect($server);
htmlerror("Could not connect to Mopra monitor host\n") if (!defined $mon);

my @monpoints = qw(mpacc.servo.coords.J2000_RA
		   mpacc.servo.coords.J2000_Dec
		   mpacc.servo.coords.AzSkyPos
		   mpacc.servo.coords.ElSkyPos
		   mpacc.servo.State
                   mpacc.servo.Target
		   mpclock.misc.clock.TickPhase
                   mpclock.misc.clock.dUTC
                   site.environment.weather.WindSpeed
		  );

my @vals = monpoll($mon, @monpoints);

print header('text/html'), start_html(-title=>'Mopra monitoring',
				      -head=>meta({-http_equiv => 'Refresh',
						   -content    => 5}),
				      -style => { -code => join('',<DATA>)}
				     );

my %site = ();

foreach (@vals) {

  if ($_->point =~ /^mpacc.servo.State$/) {
    $site{State} = $_->val;
    $site{bat} = $_->bat;
  } elsif ($_->point =~ /^mpacc.servo.Target$/) {
    $site{Target} = $_->val;
  } elsif ($_->point =~ /^mpacc.servo.coords.J2000_RA$/) {
    $site{RA} = $_->val;
  } elsif ($_->point =~ /^mpacc.servo.coords.J2000_Dec$/) {
    fixdeg($_->val);
    $site{Dec} = $_->val;
  } elsif ($_->point =~ /^mpacc.servo.coords.AzSkyPos$/) {
    fixdeg($_->val);
    $site{Az} = $_->val;
  } elsif ($_->point =~ /^mpacc.servo.coords.ElSkyPos$/) {
    fixdeg($_->val);
    $site{El} = $_->val;
  } elsif ($_->point =~ /^mpclock.misc.clock.TickPhase$/) {
    $site{TickPhase} = $_->val;
  } elsif ($_->point =~ /^mpclock.misc.clock.dUTC$/) {
    $site{dUTC} = $_->val;
  } elsif ($_->point =~ /^site.environment.weather.WindSpeed$/) {
    $site{windspeed} = $_->val;
  } else {
    print em("Ignoring $_", p);
  }
}

my $mjd = bat2mjd($site{bat});
$mjd -= $site{dUTC}/(60*60*24);

print h2('Mopra');

my $coords = sprintf("%s, %s J2000",
		     turn2str(str2turn($site{RA},'H'),'H',2),
		     turn2str(str2turn($site{Dec},'D'),'D',1));

my $azel = sprintf("%s, %s",
		   turn2str(str2turn($site{Az},'D'),'D',2),
		   turn2str(str2turn($site{El},'D'),'D',1));

print table(
	    Tr(th("Time (UT):"), td(mjd2time($mjd))),
#	    Tr(th("Source:"), td($site{Target})),
	    Tr(th("Coords:"), td($coords)),
	    Tr(th("Az/El:"), td($azel)),
	    Tr(th("Ant state:"), td($site{State})),
	    Tr(th("tickPhase:"), td($site{TickPhase})),
	    Tr(th("Wind Speed:"), td($site{windspeed}, "km/s"))
	    );

# Assume we have not yet started outputting content yet
sub htmlerror {
  print header('text/html'),
     start_html('Mopra Onsource: CGI Error'),
       h2(shift);
  exit;
}

sub fixdeg {
  $_[0] =~ s/\?/d/;
  $_[0] =~ s/"//;
  $_[0] .= '"';
}

__DATA__
th {
  background-color: #0066ff;
  color: #ffffff;
  text-align: right;
  border-style: solid;
  border-color: #0066ff;
  padding-left: 0.5em
}
th#antenna { 
  background-color: #ff6600;
  border-color: #ff6600;
  text-align: center 
}
td {
  padding-left: 1.5em 
}

