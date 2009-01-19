#!/usr/local/bin/perl -w

use strict;

use lib '/nfs/wwwatcgi/vlbi/perllib';

use CGI::Pretty qw/:standard *table *Tr/;

use Astro::Time;
use ATNF::MoniCA;

$Astro::Time::StrZero = 2;

my $server = 'monhost-nar.atnf.csiro.au';

my @ants = qw(ca01 ca02 ca03 ca04 ca05 ca06);

my $mon = monconnect($server);
htmlerror("Could not connect to ATCA monitor host\n") if (!defined $mon);

#site.misc.monitor_sys.TimeUTC

my @monpoints = qw(site.misc.obs.refAnt
		   site.misc.obs.source
		   site.misc.obs.coordSys
		   site.misc.obs.target1
		   site.misc.obs.target2
		   site.misc.obs.freq1
		   site.misc.obs.freq2
                   site.misc.obs.bw1
                   site.misc.obs.bw2
                   site.misc.obs.corrConfig
                   site.misc.obs.scanStart
                   site.misc.obs.scanEnd
                   site.misc.catie.Active
                   site.misc.catie.Mode
                   site.environment.weather.WindSpeed
                   site.environment.weather.Temperature
                   site.environment.weather.WindDir
		   caclock.misc.clock.TickPhase
                   caclock.misc.clock.dUTC
		  );

my @antmon = qw(misc.obs.caobsAntState
		misc.station
                misc.turret.feed
		servo.State
                servo.coords.AzSkyPos
                servo.coords.ElSkyPos
                misc.catie.Active1
                misc.catie.Active2
                receiver.system_temps.TsysA1
                receiver.system_temps.TsysB1
                receiver.system_temps.TsysA2
                receiver.system_temps.TsysB2
	       );

foreach my $ant (@ants) {
  foreach (@antmon) {
    push @monpoints, "$ant.$_";
  }
}

my @vals = monpoll($mon, @monpoints);

print header('text/html'), start_html(-title=>'ATCA monitoring',
				      -head=>meta({-http_equiv => 'Refresh',
						   -content    => 5}),
				      -style => { -code => join('',<DATA>)}
				     );

my %site = ();
my %ant = ();

foreach (@vals) {

  if ($_->point =~ /^site.misc.obs.refAnt$/) {
    $site{refAnt} = $_->val;
    $site{bat} = $_->bat;
  } elsif ($_->point =~ /^site.misc.obs.source$/) {
    $site{source} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.coordSys$/) {
    $site{coordSys} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.target1$/) {
    $site{target1} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.target2$/) {
    $site{target2} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.freq1$/) {
    $site{freq1} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.freq2$/) {
    $site{freq2} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.bw1$/) {
    $site{bw1} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.bw2$/) {
    $site{bw2} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.corrConfig$/) {
    $site{corrConfig} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.scanStart$/) {
    $site{scanStart} = $_->val;
  } elsif ($_->point =~ /^site.misc.obs.scanEnd$/) {
    $site{scanEnd} = $_->val;
  } elsif ($_->point =~ /^site.misc.catie.Active$/) {
    $site{catieActive} = $_->val;
  } elsif ($_->point =~ /^site.misc.catie.Mode$/) {
    $site{catieMode} = $_->val;
  } elsif ($_->point =~ /^site.environment.weather.WindSpeed$/) {
    $site{WindSpeed} = $_->val;
  } elsif ($_->point =~ /^site.environment.weather.WindDir$/) {
    $site{WindDir} = $_->val;
  } elsif ($_->point =~ /^site.environment.weather.Temperature$/) {
    $site{Temperature} = $_->val;
  } elsif ($_->point =~ /^caclock.misc.clock.TickPhase$/) {
    $site{TickPhase} = $_->val;
  } elsif ($_->point =~ /^caclock.misc.clock.dUTC$/) {
    $site{dUTC} = $_->val;
  } elsif ($_->point =~ /^(ca\d+)\./) {
    my $ant = $1;
    if ($_->point =~ /servo.State$/) {
      $ant{"$ant-State"} = $_->val;
    } elsif ($_->point =~ /misc.turret.feed$/) {
      $ant{"$ant-feed"} = $_->val;
    } elsif ($_->point =~ /misc.station$/) {
      $ant{"$ant-station"} = $_->val;
    } elsif ($_->point =~ /servo.coords.AzSkyPos$/) {
      $ant{"$ant-AzSkyPos"} = fixdeg($_->val);
    } elsif ($_->point =~ /servo.coords.ElSkyPos$/) {
      $ant{"$ant-ElSkyPos"} = fixdeg($_->val);
    } elsif ($_->point =~ /misc.obs.caobsAntState$/) {
      $ant{"$ant-AntState"} = $_->val;
    } elsif ($_->point =~ /misc.catie.Active1$/) {
      $ant{"$ant-Active1"} = $_->val;
    } elsif ($_->point =~ /misc.catie.Active2$/) {
      $ant{"$ant-Active2"} = $_->val;
    } elsif ($_->point =~ /receiver.system_temps.TsysA1$/) {
      $ant{"$ant-TsysA1"} = $_->val;
    } elsif ($_->point =~ /receiver.system_temps.TsysB1$/) {
      $ant{"$ant-TsysB1"} = $_->val;
    } elsif ($_->point =~ /receiver.system_temps.TsysA2$/) {
      $ant{"$ant-TsysA2"} = $_->val;
    } elsif ($_->point =~ /receiver.system_temps.TsysB2$/) {
      $ant{"$ant-TsysB2"} = $_->val;
    } else {
      print "Ignoring $_->point", p;
    }
  } else {
    print "Ignoring $_", p;
  }
}

my $mjd = bat2mjd($site{bat});
$mjd -= $site{dUTC}/(60*60*24);

print h2('ATCA Monitoring');

my $coords = sprintf("%s, %s %s",
		     turn2str(str2turn($site{target1},'H'),'H',2),
		     turn2str(str2turn($site{target2},'D'),'D',1),
		     $site{coordSys});

print table(
	    Tr(th("Time (UT):"), td(mjd2time($mjd)),
	      th("Scan Start"), td($site{scanStart})),
	    Tr(th("Source:"), td($site{source}),
	       th("Scan End"), td($site{scanEnd})),
	    Tr(th("Coords:"), td($coords),
	      th("refAnt"), td($site{refAnt})),
	    Tr(th("Freq 1"), td($site{freq1}),
	      th("tickPhase"), td($site{TickPhase})),
	    Tr(th("Freq 2"), td($site{freq2}),
	      th("Tied Array Active"), td($site{catieActive})),
	    Tr(th("Bandwidth 1"), td($site{bw1}),
	       th("Tied Array Mode"), td($site{catieMode})),
	    Tr(th("Bandwidth 2"), td($site{bw2}),
	       th("Wind Speed"), td($site{WindSpeed})),
	    Tr(th("Correlator"), td($site{corrConfig}),
	       th("Temperature"), td($site{Temperature})),
	   );

print hr, start_table;

print start_Tr, td();
foreach (@ants) {
  print th($_);
}
print end_Tr;

antrow('Antenna State','State');
antrow('Caobs State','AntState');
antrow('Feed','feed');

#blankrow();

antrow('Azimuth Position','AzSkyPos');
antrow('Elevation Position','ElSkyPos');

antrow('Station', 'station');

antrow('Tied Array Active - Freq 1', 'Active1');
antrow('Tied Array Active - Freq 2', 'Active2');

antrow('A1 Tsys', 'TsysA1');
antrow('B1 Tsys', 'TsysB1');
antrow('A2 Tsys', 'TsysA2');
antrow('B2 Tsys', 'TsysB2');

print end_table;

print end_html;

sub antrow {
  my ($header, $point) = @_;

  print start_Tr, th($header);
  foreach (@ants) {
    print td($ant{"${_}-$point"});
  }
  print end_Tr;
}

sub blankrow {
  print Tr(td("&nbsp;"));
}

# Assume we have not yet started outputting content yet
sub htmlerror {
  print header('text/html'),
     start_html('Mopra Onsource: CGI Error'),
       h2(shift);
  exit;
}

sub fixdeg {
  #for (my $i=0; $i<@_; $i++) {
  $_[0] =~ s/^(\d+)[^\d]([^\d]\d\d'\d\d"\.\d+)/$1$2/;
  $_[0] =~ s/\?/d/;
  $_[0] =~ s/"//;
  $_[0] .= '"';
  #}
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

