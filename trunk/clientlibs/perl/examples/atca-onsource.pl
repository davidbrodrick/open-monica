#!/usr/bin/perl -w
use strict;

# Prints an AIPS style flag file for the ATCA. If any of the currently tied antenna are not tracking, the
# data is considered invalid

use Getopt::Long;
use Astro::Time;
use ATNF::MoniCA;

sub array_state (%);
sub equaltol ($$$);
sub printflag ($$$);

$Astro::Time::StrZero = 2;
$Astro::Time::StrSep = ',';

my $server = 'monhost-nar.atnf.csiro.au';

my @ants = qw(ca01 ca02 ca03 ca04 ca05 ca06);

my $statemon = 'servo.State';

my $tiny = 0.4/60/60/24; #  (0.4sec)

my ($start, $end, $year);

GetOptions('start=s'=>\$start, 'end=s'=>\$end, 'year=i'=>\$year);

if (!defined $start || !defined $end || !defined $year) {
  die "Usage: atca-onsouce.pl -start <start> -end <end> -year <year>\n  Start/end:  ddd/hh:mm:ss\n";
}

# Need time range in mjd
my ($mjd1, $mjd2);
if ($start =~ /^(\d+)\/(\d+:\d+:\d+)$/) {
  $mjd1 = dayno2mjd($1, $year, str2turn($2, 'H'));
} else {
  die("Wrong time format for start ($start)\n");
}
if ($end =~ /^(\d+)\/(\d+:\d+:\d+)$/) {
  $mjd2 = dayno2mjd($1, $year, str2turn($2, 'H'));
} else {
  die("Wrong time format for end ($end)\n");
}

my $mon = monconnect($server);
die("Could not connect to ATCA monitor host\n") if (!defined $mon);

# Get the dUT 
my $dUT = monpoll($mon, 'caclock.misc.clock.dUTC');
$dUT = $dUT->val;


# Start 10min earlier

$mjd1 -= 10/60/24;

my @state = atca_tied($mon, $mjd1, $mjd2);

foreach my $tiedstate (@state) {
  $mjd1 = $tiedstate->[0];
  $mjd2 = $tiedstate->[1];
  my @tied_ant = @{$tiedstate->[2]};

  my $ant0 = $tied_ant[0]; # Short cut

  my $ant;
  my %vals = ();
  foreach $ant (@tied_ant) {
    my @vals = monbetween($mon, $mjd1, $mjd2, "$ant.$statemon");
    $vals{$ant} = [@vals];
  }

  my $currentbat = 0;
  # Get the latest initial state - we cannot know the state before we have a value
  # from all antenna
  ## Probably need to rethink this and take state from previous window ##
  my %ant_state = ();
  foreach $ant (@tied_ant) {
    $ant_state{$ant} = 'Unknown';
  }

  my $oldstate = 'Unknown';
  my $oldbat = $mjd1 - $dUT/60/60/24;
  $oldbat = $oldbat->bstr if (ref($mjd1) eq 'Math::BigFloat');

  my $state;
  my $firstant = 'none';
  while (1) {
    # Find the lowest next value
    my $nextbat = undef;
    foreach $ant (@tied_ant) {
      if (@{$vals{$ant}}>0) {
	if (! defined $nextbat || bat2mjd($vals{$ant}->[0]->bat, $dUT) < $nextbat) {
	  $nextbat = bat2mjd($vals{$ant}[0]->bat, $dUT)->bstr;
	  $firstant = $ant;
	}
      }
    }
    last if (!defined $nextbat);

    # Update all antenna with this bat
    foreach $ant (@tied_ant) {
      if (@{$vals{$ant}}>0) {
	if (equaltol(bat2mjd($vals{$ant}->[0]->bat, $dUT),$nextbat,$tiny)) {
	  my $v = shift @{$vals{$ant}};
	  $ant_state{$ant} = $v->val;
	}
      }
    }
    
    $state = array_state(%ant_state);

    if ($state ne $oldstate) {
      printflag($oldstate, $oldbat, $nextbat);
      $oldbat = $nextbat;
      $oldstate = $state;
    }
  }
}

sub equaltol ($$$) {
  if (abs($_[0]-$_[1])<$_[2]) {
    return(1);
  } else {
    return(0);
  }
}

sub array_state (%) {
  my %ant_state = @_;

  my %states = ();
  foreach my $state (values %ant_state) {
    $states{$state} = 1;
  }
  my @states = keys %states;
  if (@states==1) {
    return $states[0];
  } else {
    return 'Mixed';
  }
}

sub printflag ($$$) {
  my ($status, $mjd1, $mjd2) = @_;

  if (defined $status && $status ne 'TRACKING') {
    my ($dayno1, $year1, $ut1) = mjd2dayno($mjd1);
    my ($dayno2, $year2, $ut2) = mjd2dayno($mjd2);
    my $time1 = turn2str($ut1,'H', 0);
    my $time2 = turn2str($ut2,'H', 0);

    print "ant_name='ATCA' timerang=$dayno1,$time1, $dayno2,$time2 reason='$status' /\n";
  }
}


