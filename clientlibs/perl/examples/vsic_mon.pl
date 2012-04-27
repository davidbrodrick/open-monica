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

my $mon = monconnect($server);
die "Could not connect to $obs monitor host $server\n" if (!defined $mon);

my %monpoints = ('site.vlbi.vsic.VSIC1Remote' => 'vsic1Remote',
		 'site.vlbi.vsic.VSIC2Remote' => 'vsic2Remote',
		 'site.vlbi.vsic.VSIC1Code' => 'vsic1Code',
		 'site.vlbi.vsic.VSIC2Code' => 'vsic2Code'
		);

my @vals = monpoll($mon, keys(%monpoints));
my %monvals = monlist2hash(@vals, %monpoints);

printf("VSIC1Remote: %s\n", $monvals{vsic1Remote}->val);
if ($monvals{vsic1Code}->val eq '?') {
  print("VSIC1Code: unset\n");
} else {
  printf("VSIC1Code: 0x%X\n", $monvals{vsic1Code}->val);
}
printf("VSIC2Remote: %s\n", $monvals{vsic2Remote}->val);
if ($monvals{vsic2Code}->val eq '?') {
  print("VSIC2Code: unset\n");
} else {
  printf("VSIC2Code: 0x%X\n", $monvals{vsic2Code}->val);
}
