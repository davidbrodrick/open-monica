// IF YOU EDIT THIS CODE, UPDATE THE VERSION STRING progVersion[] in at_DsRPCServer.c

// c_monica.c

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

#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <netdb.h>
#include <string.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <time.h>
#include <math.h>

#include "c_monica.h"

int isConnected = 0;

/********************************************************************************
				monconnect
	This routine establishes a socket connection with the server address
	and port provided. It needs to be called before using monpoll, monpoll2
	etc. The monica struct must be defined before calling this function. 
	After calling the desired function, the connection should be closed using
	monclose().
	
	RETURN VALUE:
	When successful, the function will return the sockets file descriptor (which 
	is also placed in the struct). A negative return indicates failure and the 
	cause can be determined:
		-1 = failed to get the host by name
		-2 = failed to create the socket
		-3 = failed to disable lingering
		-4 = failed to set keep alives
		-5 = failed to connect socket
		-6 = failed to set socket to non-blocking
		
	EXAMPLE:
		int main(){
			struct monica Example;
			char server[] = "monhost-nar.atnf.csiro.au";
			int port = 8051;
			monconnect(server, port, &Example);
			//DO SOME THINGS			
			monclose(&Example);
		}
********************************************************************************/
int monconnect(char *server_add, int server_port, struct monica * MonStruct){
	int itsFileDes;
	
	//Connect to server, and return file descriptor
	itsFileDes = Connect_ToHost(server_add, server_port);
	
	//Initialise values
	strcpy((*MonStruct).server, server_add);
	(*MonStruct).port = server_port;
	(*MonStruct).fd = itsFileDes;
	
	//We dont have these values
	strncpy((*MonStruct).rawmsg,"\0",1);
	strncpy((*MonStruct).pointName,"\0",1);
	(*MonStruct).BAT = 0;
	(*MonStruct).data = 0;
	strncpy((*MonStruct).units,"\0",1);
	strncpy((*MonStruct).OK,"\0",1);

	isConnected = 1;
	
	return itsFileDes;
}

/********************************************************************************
				monraw
	This routine simply returns the raw message that moniCA sent back. Can 
	only be used after defining struct monica and calling monconnect.
	
	RETURN VALUE:
		 0 = SUCCESSFUL
		-1 = there is no socket connection (ie. call monconnect)
		-2 = error sending data to moniCA
		-3 = response from moniCA timed out
		-4 = error polling socket
		-5 = error occurred receiving data
	
	EXAMPLE:
		monraw(&Example,"poll \n 1 \n site.environment.weather.Temperature\n");
		printf("Message Back = %s\n", Example.rawmsg);
********************************************************************************/
int monraw(struct monica * MonStruct, char *MonPoints){

	int ret;
	static char BufFromMon[RESPONSE_LENGTH];
	
	//Need to check that connection is established
	if(isConnected == 1){
		//Send data
		ret = send((*MonStruct).fd, MonPoints, strlen(MonPoints), 0);
		if(ret < 0) return -2; //error sending data
			
		//wait for response
		ret = IsThereResponse((*MonStruct).fd);
		if(ret == 0) return -3; //Timeout
		if(ret < 0) return -4; //Error polling
		
		//Read the response
		ret = recv((*MonStruct).fd, BufFromMon, RESPONSE_LENGTH,0);
		if(ret < 0) return -5; //Error receiving data
		
		//Copy raw message into buffer
		strncpy((*MonStruct).rawmsg, BufFromMon, sizeof(BufFromMon));
		
		//We dont have these values
		strncpy((*MonStruct).pointName,"\0",1);
		(*MonStruct).BAT = 0;
		(*MonStruct).data = 0;
		strncpy((*MonStruct).units,"\0",1);
		strncpy((*MonStruct).OK,"\0",1);
	}	
	else{
		//We dont have these values
		strncpy((*MonStruct).rawmsg,"\0",1);
		strncpy((*MonStruct).pointName,"\0",1);
		(*MonStruct).BAT = 0;
		(*MonStruct).data = 0;
		strncpy((*MonStruct).units,"\0",1);
		strncpy((*MonStruct).OK,"\0",1);
		//printf("Not connected to server\n");
		isConnected = 0;
		return -1;
	}
	
	return 0;
}

/********************************************************************************
				monpoll
	This routine implements the moniCA poll command. The poll command asks the 
	server to return the most-recent value for a monitor points. The function 
	requires the monica struct to fill up and the monitor point as input. It
	then attempts to decode the returned rawmsg.
	
	RETURN VALUE:
		 0 = SUCCESSFUL
		-1 = there is no socket connection (ie. call monconnect)
		-2 = error sending data to moniCA
		-3 = response from moniCA timed out
		-4 = error polling socket
		-5 = error occurred receiving data
		-6 = error decoding received data
	
	EXAMPLE:
		monpoll(&Example, "caclock.misc.clock.dUTC");
		printf("Message Back = %s\n", Example.rawmsg);
		printf("point = %s\n", Example.pointName);
		printf("BAT = %llx\n", Example.BAT);
		printf("data = %f\n", Example.data);
********************************************************************************/
int monpoll(struct monica * MonStruct, char *MonPoints){
	int ret;
	static char BufFromMon[RESPONSE_LENGTH];
	char ToSend[SEND_LENGTH];
	
	//build the string
	strcpy(ToSend, "poll\n1\n");
	strcat(ToSend, MonPoints);
	strcat(ToSend, "\n");
	
	//Need to check that connection is established
	if(isConnected == 1){
		//Send data
		ret = send((*MonStruct).fd, ToSend, strlen(ToSend), 0);
		if(ret < 0) return -2; //error sending data
			
		//wait for response
		ret = IsThereResponse((*MonStruct).fd);
		if(ret == 0) return -3; //Timeout
		if(ret < 0) return -4; //Error polling
		
		//Read the response
		ret = recv((*MonStruct).fd, BufFromMon, RESPONSE_LENGTH,0);
		if(ret < 0) return -5; //Error receiving data
		
		//Copy raw message into buffer
		strncpy((*MonStruct).rawmsg, BufFromMon, sizeof(BufFromMon));
		
		ret = DecodePoll(BufFromMon, MonStruct);
		if(ret < 0) return -6; //Error Decoding
		
		//We dont have these values
		strncpy((*MonStruct).units,"\0",1);
		strncpy((*MonStruct).OK,"\0",1);
		
	}	
	else{
		//We dont have these values
		strncpy((*MonStruct).rawmsg,"\0",1);
		strncpy((*MonStruct).pointName,"\0",1);
		(*MonStruct).BAT = 0;
		(*MonStruct).data = 0;
		strncpy((*MonStruct).units,"\0",1);
		strncpy((*MonStruct).OK,"\0",1);
		//printf("Not connected to server\n");
		isConnected = 0;
		return -1;
	}
	
	
	return 0;
}

/********************************************************************************
				monpoll2
	This routine is similar to the poll command except that the response from 
	the server also includes the data units and a boolean indicating whether 
	the current value is within acceptable limits or not. The function attempts
	to decode this response.
	
	RETURN VALUE:
		 0 = SUCCESSFUL
		-1 = there is no socket connection (ie. call monconnect)
		-2 = error sending data to moniCA
		-3 = response from moniCA timed out
		-4 = error polling socket
		-5 = error occurred receiving data
		-6 = error decoding received data
	
	EXAMPLE:
		monpoll2(&Example, "caclock.misc.clock.dUTC");
		printf("Message Back = %s\n", Example.rawmsg);
		printf("point = %s\n", Example.pointName);
		printf("BAT = %llx\n", Example.BAT);
		printf("data = %f\n", Example.data);
		printf("units = %s\n", Example.units);
		printf("OK = %s\n", Example.OK);	
********************************************************************************/
int monpoll2(struct monica * MonStruct2, char *MonPoints2){

	int ret;
	static char BufFromMon2[RESPONSE_LENGTH];
	char ToSend2[SEND_LENGTH];

	//build the string
	strcpy(ToSend2, "poll2\n1\n");
	strcat(ToSend2, MonPoints2);
	strcat(ToSend2, "\n");


	//Need to check that connection is established
	if(isConnected == 1){
		//Send data
		ret = send((*MonStruct2).fd, ToSend2, strlen(ToSend2), 0);
		if(ret < 0) return -2; //error sending data
	
		//wait for response
		ret = IsThereResponse((*MonStruct2).fd);
		if(ret == 0) return -3; //Timeout
		if(ret < 0) return -4; //Error polling
		
		//Read the response
		ret = recv((*MonStruct2).fd, BufFromMon2, RESPONSE_LENGTH,0);
		if(ret < 0) return -5; //Error receiving data
		
		//Copy raw message into buffer
		strncpy((*MonStruct2).rawmsg, BufFromMon2, sizeof(BufFromMon2));
		
		ret = DecodePoll2(BufFromMon2, MonStruct2);
		if(ret < 0) return -6; //Error Decoding
	}	
	else{
		//We dont have these values
		strncpy((*MonStruct2).rawmsg,"\0",1);
		strncpy((*MonStruct2).pointName,"\0",1);
		(*MonStruct2).BAT = 0;
		(*MonStruct2).data = 0;
		strncpy((*MonStruct2).units,"\0",1);
		strncpy((*MonStruct2).OK,"\0",1);
		
		//printf("Not connected to server\n");
		isConnected = 0;
		return -1;
	}
	
	
	return 0;
}

/********************************************************************************
				monsince
	This routine is functioning but is not implemented in a useful way.
	The since command returns all records, for a single monitor point, between 
	the nominated time and the present. The library currently limits the number
	of characters for the moniCA response message, so if the number of records 
	requested is too large you wont get the full response
********************************************************************************/
/*
int monsince(struct monica * MonStruct, char *MonPoints, unsigned long long sinceBAT){

	int ret;
	static char BufFromMon[RESPONSE_LENGTH];
	char BAT_str[20];
	char ToSend[SEND_LENGTH];
	
	//build the string
	strcpy(ToSend, "since\n");
	sprintf(BAT_str,"0x%llx\t", sinceBAT);
	strcat(ToSend, BAT_str);
	strcat(ToSend, MonPoints);
	strcat(ToSend, "\n");


	//Need to check that connection is established
	if(isConnected == 1){
		//Send data
		ret = send((*MonStruct).fd, ToSend, strlen(ToSend), 0);
		if(ret < 0) return -2; //error sending data
			
		//wait for response
		ret = IsThereResponse((*MonStruct).fd);
		if(ret == 0) return -3; //Timeout
		if(ret < 0) return -4; //Error polling
		
		//Read the response
		ret = recv((*MonStruct).fd, BufFromMon, RESPONSE_LENGTH,0);
		if(ret < 0) return -5; //Error receiving data
		
		//Copy raw message into buffer
		strncpy((*MonStruct).rawmsg, BufFromMon, sizeof(BufFromMon));

		strncpy((*MonStruct).pointName,"\0",1);
		(*MonStruct).BAT = 0;
		(*MonStruct).data = 0;
		strncpy((*MonStruct).units,"\0",1);
		strncpy((*MonStruct).OK,"\0",1);		
	
	}	
	else{
		//We dont have these values
		strncpy((*MonStruct).rawmsg,"\0",1);
		strncpy((*MonStruct).pointName,"\0",1);
		(*MonStruct).BAT = 0;
		(*MonStruct).data = 0;
		strncpy((*MonStruct).units,"\0",1);
		strncpy((*MonStruct).OK,"\0",1);
		
		printf("Not connected to server\n");
		isConnected = 0;
		return -1;
	}
	
	
	return 0;
}
*/

/********************************************************************************
				BAT2human
	This routine takes the BAT provided and converts it to a readable 
	date/time. It requires the struct mon_time, dUTC and a timezone offset.
	
	RETURN VALUE:
		No return value
	
	EXAMPLE:
		struct mon_time time;		
		BAT2human(Example.BAT, 0x22,11, &time);
		printf("%02d/%02d/%04d %02d:%02d:%02d.%06d\n", time.day, time.month, 
						time.year, time.hour, time.min, time.sec,time.usec);
********************************************************************************/
void BAT2human(unsigned long long bat, int dutc, int ofs, struct mon_time * timeIn){
 
	unsigned long long frac;
	double timegm;
	struct tm *time;
	time_t clock;
		
	if (dutc<30) printf("WARNING: dUTC is less than expected!\n");
	
	frac = bat % (unsigned long long)1e6; // get frac of second
	
	timegm = (double)bat / (double)1e6; // convert to seconds
	timegm -= dutc; // apply dUTC offset
	timegm -= (double)40587 * (double) 86400; // convert mjd to seconds since Jan 1, 1970 (do as double to get around integer round off prob)

	clock= timegm + (time_t) ofs * (time_t) 3600; // convert to time_t & add TZ offset as required

	time = gmtime(&clock);

	(*timeIn).day = time->tm_mday;
	(*timeIn).month = time->tm_mon+1;
	(*timeIn).year = time->tm_year+1900;
	(*timeIn).hour = time->tm_hour;
	(*timeIn).min = time->tm_min;
	(*timeIn).sec = time->tm_sec;
	(*timeIn).usec = frac;
	
}

/********************************************************************************
				BAT2human_print
	Similar to BAT2human, but just prints the data/time to screen
	
	RETURN VALUE:
		No return value
********************************************************************************/
void BAT2human_print(unsigned long long bat, int dutc, int ofs){
 
	unsigned long long frac;
	double timegm;
	struct tm *time;
	time_t clock;
		
	if (dutc<30) printf("WARNING: dUTC is less than expected!\n");
	
	frac = bat % (unsigned long long)1e6; // get frac of second

	timegm = (double)bat / (double)1e6; // convert to seconds
	timegm -= dutc; // apply dUTC offset
	timegm -= (double)40587 * (double) 86400; // convert mjd to seconds since Jan 1, 1970 (do as double to get around integer round off prob)

	clock= timegm + (time_t) ofs * (time_t) 3600; // convert to time_t & add TZ offset as required

	time = gmtime(&clock);
	printf("%02d/%02d/%04d %02d:%02d:%02d.%06lld\n", time->tm_mday, time->tm_mon+1, time->tm_year+1900, time->tm_hour, time->tm_min, time->tm_sec, frac /*, time->tm_isdst*/);

}

/********************************************************************************
				human2BAT
	This routine takes the human date and returns its BAT.
	
	RETURN VALUE:
		Returns the BAT if successful, otherwise 0;
	
	EXAMPLE:
		unsigned long long myBAT;
		myBAT = human2BAT("10/12/2010","14:27:12.22910", 0x22);
********************************************************************************/
unsigned long long human2BAT(char * dateIn, char * timeIn, int dUTC){

	unsigned long long bat =0, sec;
	unsigned long long bat_sec;
	struct tm time_str;

	if(strnlen(dateIn,12)!=10){
		//Do not have correct number of characters. FORMAT: XX/XX/XXXX
		return 0;
	}
	
	//get date
	int day = 0, month = 0, year = 0;
	int hours = 0, mins = 0, secs = 0, usecs = 0;
	char *day_s = strtok (dateIn,"/");
	char *month_s = strtok(NULL, "/");
	char *year_s = strtok(NULL, "/");

	//convert to integers
	day = atoi(day_s);
	month = atoi(month_s);
	year = atoi(year_s);
	
	if(strcspn(timeIn,":") >= strlen(timeIn) ){
		//we don't have any colons, incorrect format
		return 0;
	}
	if(strcspn(timeIn,".") >= strlen(timeIn) ){
		//we don't have any dots, incorrect format
		return 0;
	}
	//get time
	char *hours_s = strtok (timeIn,":");
	char *minutes_s = strtok(NULL, ":");
	char *seconds_s = strtok(NULL, ".");
	char *useconds_s = strtok(NULL, "\0");

	//convert to integers
	hours = atoi(hours_s);
	mins = atoi(minutes_s);
	secs = atoi(seconds_s);
	usecs = atoi(useconds_s);

    time_str.tm_year = year - 1900; //years since 1900
    time_str.tm_mon = month - 1; //months since January
    time_str.tm_mday = day;
    time_str.tm_hour = hours;
    time_str.tm_min = mins;
    time_str.tm_sec = secs;
    time_str.tm_isdst = -1; //is daylight saving
	
	bat_sec = mktime(&time_str);
	
	sec = (unsigned long long)bat_sec + (unsigned long long) 40587 * (unsigned long long) 86400 + (unsigned long long) dUTC;
	bat = sec * (unsigned long long)1000000 + (unsigned long long) usecs;

	return bat;
}

/********************************************************************************
				monclose
	This function closes the socket connection using the file descriptor
	associated with the monica struct which was assigned during monconnect().
	
********************************************************************************/
int monclose(struct monica * MonStruct){
		
	//Close the socket connection
	close((*MonStruct).fd);	
	isConnected = 0;
	
	return 0;
}

/********************************************************************************
			THIS FUNCTION IS USED INTERNALLY			
********************************************************************************/
int DecodePoll(char * msg, struct monica * MonStruct0){

	//check that BAT is present
	char * BATLoc = strstr(msg, "0x");

	if(BATLoc != NULL){
		//BAT is present
		//Each field is seperated by a tab
		char* ConnectionPoint = strtok(msg, "\t");
		char* BAT_string = strtok(NULL, "\t");
		char* data_string = strtok(NULL, "\n");
		
		//assign values to structure
		strcpy((*MonStruct0).pointName, ConnectionPoint);
		
		//convert BAT to unsigned long long
		(*MonStruct0).BAT = strtoull(BAT_string, NULL, 16);
		
		//convert data to float
		(*MonStruct0).data = atof(data_string);

	}
	else{
		//No BAT found
		return -1;
	}
	
	return 0;
	

}

/********************************************************************************
			THIS FUNCTION IS USED INTERNALLY
********************************************************************************/
int DecodePoll2(char * msg, struct monica * MonStruct0){

	//check that BAT is present
	char * BATLoc = strstr(msg, "0x");

	if(BATLoc != NULL){
		//BAT is present
		//Each field is seperated by a tab
		char* ConnectionPoint = strtok(msg, "\t");
		char* BAT_string = strtok(NULL, "\t");
		char* data_string = strtok(NULL, "\t");
		char* units_string = strtok(NULL, "\t");
		char* OK_string = strtok(NULL, "\n");
		
		//assign values to structure
		strcpy((*MonStruct0).pointName, ConnectionPoint);
		
		//convert BAT to unsigned long long
		(*MonStruct0).BAT = strtoull(BAT_string, NULL, 16);
		
		//convert data to float
		(*MonStruct0).data = atof(data_string);
		
		//assign units
		strcpy((*MonStruct0).units, units_string);
		
		//assign OK
		strcpy((*MonStruct0).OK, OK_string);
	}
	else{
		//No BAT found
		return -1;
	}
	return 0;
}

/********************************************************************************	
			THIS FUNCTION IS USED INTERNALLY
********************************************************************************/
int Connect_ToHost(char * host, int port){
	
	int err,ret;
	struct sockaddr_in sa_time;
	struct hostent *phost;
	struct linger ling;
	
	//Our file descriptor
	int mon_fd;
	int SocketStatus;

	// BEGIN
	if ((phost = gethostbyname(host)) == NULL){
		//printf("gethostbyname for time failed: \t");
		//printf("host = '%s' is invalid \n", host);
		return -1;
	}
	// Create socket 
	// AF_ = Address family   PF_ = Protocol family
	if ((mon_fd = socket(PF_INET, SOCK_STREAM, 0)) < 0){
		//printf("Establishing socket ... ");
		//printf("ERROR: %s\n", strerror(errno));
		return -2;
	}

	// Disable any lingering on close.  When a close is done there is nothing
	// we need to get through to the remote end.
	ling.l_onoff = 0;
	ling.l_linger = 0;
	err = setsockopt(mon_fd, SOL_SOCKET, SO_LINGER, &ling, sizeof(ling));
	if (err < 0) {
		//printf("An error occured setting socket options \n");
		shutdown(mon_fd,SHUT_RDWR);
		return -3;
	}
	
	// Turn on keep alives, these are notified via SIGPIPE...
	// So we need a SIGPIPE handler...
	ret = 1;
	err = setsockopt(mon_fd, SOL_SOCKET, SO_KEEPALIVE, &ret, sizeof(ret));
	if (err < 0){
		//printf("An error occured setting socket options \n");
		shutdown(mon_fd, SHUT_RDWR);
		return -4;
	}
	
	// Open the connection 
	memset((void *)&sa_time, '\0', sizeof(struct sockaddr_in)); // set sa_time block to 0
	memcpy(&sa_time.sin_addr, phost->h_addr, phost->h_length);

	sa_time.sin_family = phost->h_addrtype;
	sa_time.sin_port = htons(port);

	//Connect Socket
	err = connect(mon_fd, (struct sockaddr *) &sa_time, (int) sizeof(sa_time));
	if (err >= 0) SocketStatus = 1;
	if (err < 0){
		//printf("An error occured connecting to : %s \n", host);
		shutdown(mon_fd,SHUT_RDWR);
		SocketStatus = 0;
		return -5;
	}
	
	// Set non-blocking for all operations on this socket.
    // This will prevent hanging on read() or write()
	err = fcntl(mon_fd, F_SETFL, O_NONBLOCK);
	if (err < 0) 
	{
		//printf("An error occured setting file status flag to non blocking \n");
		shutdown(mon_fd,SHUT_RDWR);
		return -6;
	}
	
	return mon_fd;
}

/********************************************************************************
			THIS FUNCTION IS USED INTERNALLY
********************************************************************************/
int IsThereResponse(int OurFD)
{
	const int timeout = MONICA_TIMEOUT;
	struct pollfd ufd; 
	const nfds_t numfd = 1;
	ufd.fd = OurFD;
	ufd.events = POLLIN;
	int err;
	
	err = poll (&ufd, numfd, timeout);
	
	if (err == 0 || (ufd.revents & POLLERR) || (ufd.revents & POLLHUP)){
		// No Activity
		return 0;  
	}
	else if (err < 0){
		// Error
		return -1;  
	}
	//Activity
	return 1;  
}



