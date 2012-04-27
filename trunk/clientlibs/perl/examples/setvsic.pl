#!/usr/local/bin/perl -w

use strict;

use Astro::Time;
use ATNF::MoniCA;
use Getopt::Long;

my $obs;
$obs = $ENV{VSICOBS} if (defined $ENV{VSICOBS});

my $atca = 0;
my $parkes = 0;
my $mopra = 0;

GetOptions('atca'=>\$atca, 'mopra'=>\$mopra, 'parkes'=>\$parkes);

if ($atca) {
  die "Specific just one observatory" if ($parkes || $mopra);
  $obs = 'ATCA';
} elsif ($mopra) {
 die "Specific just one observatory" if ($parkes);
  $obs = 'MOPRA';
} elsif ($parkes) {
  $obs = 'PARKES';
}

die "No observatory defined\n" if (!defined $obs);

$obs = uc($obs);

my $server;
if ($obs eq 'ATCA') {
  $server = 'monhost-nar';
} elsif ($obs eq 'MOPRA') {
  $server = 'monhost-mop';
} elsif ($obs eq 'PARKES') {
  $server = 'monhost-pks';
} else {
  die "Unrecognised observatory $obs\n";
}

die "Usage: setvsic.pl <MODE>\n" if (@ARGV != 1);

my $code = hex(shift @ARGV);

my $mon = monconnect($server);
die "Could not connect to $obs MoniCA host \"$server\"\n" if (!defined $mon);

my @setpoints;

push @setpoints, MonSetPoint({point => 'site.vlbi.vsic.control.Code',
			      val => $code,
			      type => 'int'});


my $ret = monset($mon, 'a', 'b', @setpoints);

print $ret->val, "\n";
