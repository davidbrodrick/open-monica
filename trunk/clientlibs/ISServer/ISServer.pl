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
# This is the generic data server which serves data from any file in the data directory to 
# the MoniCA server. 
# To retrieve the data in the file named "data", connect to this server and send the
# string "data\r\n". This will then read the contents of that file and send the data.
# To disconnect, send the string "quit\r\n", "bye\r\n" or "exit\r\n".
# To retrieve all data that's available in every file in the data directory,
# send the command "all\r\n". This will prepend each data point in each data file
# with the name of the file it was read from. Example:
# If the file "data" has a datapoint named "supply voltage", the command "all"
# will return the datapoint "data-supply voltage", whereas the command "data"
# will only return the "supply voltage"
#
# The syntax for the data files is simple: Each line contains the data point name,
# the data point value and the data point unit. They are delineated by tab. Example:
# Flux gate capacitor voltage  \t   235.2     \t V
# just make sure the \t are in fact tabs, not the string \\t...
#
# *************************************************************
# Installation instructions
# *************************************************************
# 1. Adjust the port, datahome and logdir directories to suit your needs.
# 2. datahome is where your data gatherer saves the data. Refer to the Gather_Sample_Data.pl script for an example.
# 3. Run this manually to make sure there are no error messages
# 4. Install it as a cronjob to start every minute. It'll only start one instance.
# *************************************************************

use IO::Socket;
use Net::hostent;              # for OO version of gethostbyaddr
use Fcntl qw(LOCK_EX LOCK_NB);
use File::NFSLock;

use strict;

my $client;
my $PORT = 7111 ;               # pick any port, 
my $peer;
my $datahome = "/Users/secundus/MoniCA/data";            # the directory where the data files are kept
my $logdir = "/Users/secundus/MoniCA/";              # the directory where loggin is done
my $modified;
my $file, my @files;
my $push = 0;
my @tstamp;
my $version = "v.1.1 19/08/2012 balt\@inside.net";

# Try to get an exclusive lock on myself.
my $lock = File::NFSLock->new($0, LOCK_EX|LOCK_NB);
die "$0 is already running!\n" unless $lock;


my $server = IO::Socket::INET->new( Proto     => 'tcp',
                                  LocalPort => $PORT,
                                  Listen    => SOMAXCONN,
                                  Reuse     => 1);

die "Can't start server. Is the port $PORT free?" unless $server;

sub logger ($) {
  my $tmp = join "", @_;
  chomp $tmp;
  my $add = index($tmp, ":");
  my $head = scalar(gmtime) . " UTC > ";
  my $gap = " " x (length($head) + $add + 6);
  $tmp =~ s/\n/\n$gap/g;
  open FH, "+>>$logdir/ISServer.log";
  print FH "" . $head . $tmp . "\n";
  close FH;
}

logger "*********************************************************************";
logger "Inside Systems Server started. $version";
logger "*********************************************************************";

while ($client = $server->accept()) {
   $client->autoflush(1);
   $peer = $client->peerhost();
   logger "Connect from $peer";
   
   while ( <$client>) {
     if ( $push ) {
       # fetch and compare timestamps, if any changed, push data
sleeper:
       select(undef, undef, undef, 5.0);
       goto readall;
     }
     chop($_);     
     logger "Command received: $_";
     next unless /\S/;       # blank line
     if (/quit|exit|bye/i) {
       last; 
     } elsif (-e "$datahome/$_" ) {
       print $client `cat $datahome/$_`;  
       print $client "Done\n"; 
     } elsif (/push/i) {
       $push = 1;
     } elsif (/all/i) {

readall:

       # get file listing and print the contents of them all
       opendir(DIR, $datahome);
       @files = readdir(DIR);
       closedir(DIR);
       
       foreach $file (@files) {
         next unless $file =~ m/data_/;
         open IN, "<$datahome/$file";
	 while (<IN>) {
	   print $client "$file-$_";
	 }
	 close IN;
       }
       if ( $push ) {
         goto sleeper;
       }

       print $client "Done\n"; 
     } else { 
       print $client "NO DATA\n"; 
       print $client "Done\n";
     }
     
   } continue {
      print $client "";
   }
   
   $peer = $client->peerhost();
   logger "Disconnect from $peer";
   close $client;
 }

