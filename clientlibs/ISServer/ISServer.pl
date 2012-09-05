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
# To retrieve the data in the file named "data", connect to this server and send any
# string terminated by "\n". This will trigger the read command and you will be served
# the contents of any file in the data directory which commences by the name "data_".
#
# *************************************************************
# Installation instructions
# *************************************************************
# 1. Adjust the port, datahome and logdir directories to suit your needs.
# 2. datahome is where your data gatherer saves the data. Refer to the Gather_Sample_Data.pl script for an example.
# 3. Run ISServer.pl manually and look at any output as well as the log file to make sure there are no error messages
# 4. Install it as a cronjob to start every minute. It'll only start one instance.
# *************************************************************

use POE qw(Component::Server::TCP);
use Fcntl qw(LOCK_EX LOCK_NB);
use File::NFSLock;

use strict;

my $PORT = 7111 ;                                 # pick any port, 
my $datahome = "/Users/secundus/MoniCA/data";     # the directory where the data files are kept
my $logdir = "/Users/secundus/MoniCA/";           # the directory where loggin is done
my $modified;
my $file, my @files;
my $push = 0;
my @tstamp;
my $version = "v.2.0 05/09/2012 balt\@inside.net";

# Try to get an exclusive lock on myself to prevent multiple starts.
my $lock = File::NFSLock->new($0, LOCK_EX|LOCK_NB);
die "$0 is already running!\n" unless $lock;

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
logger "IS Server started. $version";
logger "*********************************************************************";

# Start a TCP server.  Client input will be logged to the console and
# echoed back to the client, one line at a time.

POE::Component::Server::TCP->new(
  Alias       => "ISServer",
  Port        => $PORT,
  ClientInput => sub {
    my ($session, $heap, $input) = @_[SESSION, HEAP, ARG0];
    logger "Session " . $session->ID() . " RX: $input\n";
      
    # get file listing and print the contents of them all
      opendir(DIR, $datahome);
      @files = readdir(DIR);
      closedir(DIR);
      
      my $output = "";
      
      foreach $file (@files) {
        next unless $file =~ m/data_/;
        open IN, "<$datahome/$file";
        while (<IN>) {
          $output = $output . "$file-$_";
        }
        close IN;
      }   
      $heap->{client}->put($output);
  }
);

# Start the server.

$poe_kernel->run();
exit 0;

