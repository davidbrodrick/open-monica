// c_monica.h

// Copyright (C)1997  CSIRO Australia Telescope National Facility
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Library General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Library General Public License for more details.
//
// A copy of the GNU Library General Public License is available at:
//     http://wwwatoms.atnf.csiro.au/doc/gnu/GLGPL.htm
// or, write to the Free Software Foundation, Inc., 59 Temple Place,
// Suite 330, Boston, MA  02111-1307  USA

//define amount of time to wait for response from monica
#define MONICA_TIMEOUT 100 //ms

#define HOST_LENGTH 255
#define SEND_LENGTH 255
#define RESPONSE_LENGTH 255

//#define NARR_SERVER "monhost-nar.atnf.csiro.au"
//#define MOP_SERVER "monhost-mop.atnf.csiro.au"
//#define PKS_SERVER "monhost-pks.atnf.csiro.au"

//#define NARR_POINT "poll \n 1 \n caclock.misc.clock.dUTC \n"
//#define MOP_POINT "poll \n 1 \n mpclock.misc.clock.dUTC \n"
//#define PKS_POINT "poll \n 1 \n pkclock.misc.clock.dUTC \n"

//#define NARR_PORT 8051
//#define MOP_PORT 8051
//#define PKS_PORT 8051

#define SERVER_LENGTH 255
#define POINTS_LENGTH 255

//A general struct to use with monica functions
//If the point being queried doesnt return one or
//more of these variables, it will be set to 0 or NULL
struct monica{
	//send values
	char server[SERVER_LENGTH];
	int port;
	int fd;

	//return values 
	char rawmsg[RESPONSE_LENGTH];
	char pointName[POINTS_LENGTH];
	unsigned long long BAT;
	float data;
	char units[10];
	char OK[6];
};

//Struct used for converting BAT
struct mon_time{
    int year;
	int month;
	int day;
	int hour;
	int min;
	int sec;
	int usec;
};


/********************************************************************************
				Library internal routines
********************************************************************************/
int DecodePoll(char * msg, struct monica * MonStruct0);
int DecodePoll2(char * msg, struct monica * MonStruct0);
int IsThereResponse(int ourFD);
int Connect_ToHost(char * host, int port);


/********************************************************************************
				User routines
********************************************************************************/
int monconnect(char *server_add, int server_port, struct monica * MonStruct);
int monraw(struct monica * MonStruct, char *MonPoints);
int monpoll(struct monica * MonStruct, char * MonPoints);
int monpoll2(struct monica * MonStruct2, char * MonPoints2);
int monsince(struct monica * MonStruct, char *MonPoints, unsigned long long sinceBAT);
int monclose(struct monica * MonStruct);

void BAT2human(unsigned long long bat, int dutc, int ofs, struct mon_time * timeIn);
void BAT2human_print(unsigned long long bat, int dutc, int ofs);
unsigned long long human2BAT(char * dateIn, char * timeIn, int dUTC);





