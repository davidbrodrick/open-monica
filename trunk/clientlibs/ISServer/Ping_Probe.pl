#!/usr/bin/perl -w
# *************************************************************
#
# Copyright (C) 2012 Inside Systems Pty Ltd
#
# *************************************************************
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU Library General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# *************************************************************
# Description
# *************************************************************
# runs pings and saves the result in a format compatible with open-monica
# and ISServer delivery
# *************************************************************
# Installation instructions
# *************************************************************
# 1. Adjust the datahome, target list parameters to suit your needs.
# 2. datahome is where ISServer.pl will serve the data from.
# 3. Run this manually to make sure there are no error messages
# 4. Install it as a cronjob to start every minute. It'll only start one instance.
# *************************************************************

use List::MoreUtils qw(firstidx);
use strict;
use Fcntl qw(LOCK_EX LOCK_NB);
use File::NFSLock;

my $datahome = "/Users/secundus/MoniCA/data";            # the directory where the data files are kept
my @targets = ('192.168.0.1');
my $timeout = 5;   # how many seconds to time out in (note this will interrupt pings if they take longer since they are sent at a rate of only 1 ping/s!)
my $count = 5;     # how many pings to send.

my @temp;
my $temp;
my ($min, $avg, $max, $std);

my $lock = File::NFSLock->new($0, LOCK_EX|LOCK_NB);
die "$0 is already running!\n" unless $lock;

print "Ping probe running... press <ctrl>-c to quit.\n";

while (1) {
  # loop through the target list
  foreach (@targets) {
    @temp = `/sbin/ping -c $count -t $timeout -n -q $_`;
    chomp $temp[$#temp];
    @temp = split / +/, $temp[$#temp];
    if ( $temp[0] =~ m/round-trip/ ) {
      @temp = split /\//, $temp[3];
      $min = $temp[0];
      $avg = $temp[1];
      $max = $temp[2];
      $std = $temp[3];     
    } else {
      $min = $avg = $max = $std = 0;
    }
  
    open FH, "+>$datahome/data_ping_$_";
    print FH "Min\t$min\tFloat\n";
    print FH "Avg\t$avg\tFloat\n";    
    print FH "Max\t$max\tFloat\n";    
    print FH "Std\t$std\tFloat\n";        
    close FH;
  }    
}
