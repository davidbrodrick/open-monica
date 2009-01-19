#!/usr/bin/env perl -w

use strict;

use ATNF::MoniCA;

my $server = 'monhost-nar.atnf.csiro.au';

my $mon = monconnect($server);
die "Could not connect to monitor host $server\n" if (!defined $mon);

my @points = qw(site.environment.weather.Temperature);
	      
my $point = monpoll($mon, @points);

print "ATCA temperature is ", $point->val, "\n";
