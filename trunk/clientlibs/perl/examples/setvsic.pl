#!/usr/local/bin/perl -w

use strict;

use Astro::Time;
use ATNF::MoniCA;

my $server = 'monhost-nar';

die "Usage: setvsic.pl <MODE>\n" if (@ARGV != 1);

my $code = hex(shift @ARGV);

my $mon = monconnect($server);
die "Could not connect to monitor host \"$server\"\n" if (!defined $mon);

my @setpoints;

push @setpoints, MonSetPoint({point => 'site.vlbi.vsic.control.Code',
			      val => $code,
			      type => 'int'});


my $ret = monset($mon, 'a', 'b', @setpoints);


print $ret->val, "\n";
