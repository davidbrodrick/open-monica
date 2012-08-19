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
# This is an example script which gathers some system data
# such as CPU load, network traffic and more and provides it
# as properly formatted data source to ISServer in the data
# directory
#
# *************************************************************
# Installation instructions
# *************************************************************
# 1. Adjust the datahome, ifname, and dm1 through 3 parameters to suit your needs.
# 2. datahome is where your data gatherer saves the data (and where ISServer.pl will serve the data from).
# 3. Run this manually to make sure there are no error messages
# 4. Install it as a cronjob to start every minute. It'll only start one instance.
# *************************************************************

use List::MoreUtils qw(firstidx);
use strict;
use Fcntl qw(LOCK_EX LOCK_NB);
use File::NFSLock;

my $datahome = "/Users/secundus/MoniCA/data";            # the directory where the data files are kept
my $ifname = "en0";                 # name of the network interface you want to gather stats on
my $dm1 = "/";                      # disk mounts to check for free space
my $dm2 = "/Volumes/Backup";
my $dm3 = "/Volumes/Monthly\\ Backup";
my @temp;
my $temp;
my $ncon;
my $i;
my ($cpu1, $cpu2, $cpu3);
my ($df1, $df2, $df3);
my $users;

my $lock = File::NFSLock->new($0, LOCK_EX|LOCK_NB);
die "$0 is already running!\n" unless $lock;



print "Gathering data... press <ctrl>-c to quit.\n";

while (1) {
  # obtain the network packets i/o
  @temp = `/usr/sbin/netstat -I $ifname -n | /usr/bin/awk '{print \$5, \$7}'`;
  chomp $temp[1];
  @temp = split / +/, $temp[1];
  $ncon = `/usr/sbin/netstat -a -n -p tcp | grep ESTABLISHED | wc -l`;
  chomp $ncon;
  
  open FH, "+>$datahome/data_network";
  print FH "Packets In\t$temp[0]\tLong\n";
  print FH "Packets Out\t$temp[1]\tLong\n";    
  print FH "Established TCP Connections\t$ncon\tInteger\n";  
  close FH;
  
  # obtain the number of users connected to the system and the CPU use
  $temp = `/usr/bin/uptime`;
  chomp $temp;
  @temp = split / +/, $temp;
  $i = 0;
  foreach (@temp) {
    if ( $_ =~ /users/ ) {
      $users = $temp[$i-1];
    }
    
    if ( $_ =~ /averages/ ) {
      $cpu1 = $temp[$i+1];
      $cpu2 = $temp[$i+2];
      $cpu3 = $temp[$i+3];      
    }

    $i++;
  }
  open FH, "+>$datahome/data_system";
  print FH "Users\t$users\tInteger\n";
  print FH "CPU1\t$cpu1\tFloat\n";  
  print FH "CPU2\t$cpu2\tFloat\n";  
  print FH "CPU3\t$cpu3\tFloat\n";    
  close FH;
  
  # Free disk space (512 k blocks and inodes)
  @temp = `/bin/df -i $dm1 | /usr/bin/awk '{print \$4, \$7}'`;
  chomp $temp[1];
  @temp = split / +/, $temp[1];
  
  open FH, "+>$datahome/data_diskfree";
  print FH "disk1_blocks\t$temp[0]\tLong\n";
  print FH "disk1_inodes\t$temp[1]\tLong\n";    

  @temp = `/bin/df -i $dm2 | /usr/bin/awk '{print \$4, \$7}'`;
  chomp $temp[1];
  @temp = split / +/, $temp[1];
  
  print FH "disk2_blocks\t$temp[0]\tLong\n";
  print FH "disk2_inodes\t$temp[1]\tLong\n";    

  @temp = `/bin/df -i $dm3 | /usr/bin/awk '{print \$4, \$7}'`;
  chomp $temp[1];
  @temp = split / +/, $temp[1];
  
  print FH "disk3_blocks\t$temp[0]\tLong\n";
  print FH "disk3_inodes\t$temp[1]\tLong\n";    

  close FH;
  
  sleep 1;
}
