/*
    $Id: tsl.c,v 1.10 2002/05/12 22:56:47 james Exp $

    tsl, temperature sensor data logger
    Copyright (C) 2000  James Cameron (quozl@us.netrek.org)
    Butchered by David Brodrick 2008.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/time.h>
#include <ctype.h>

#define MAXSENSOR 4

#ifdef TSL_DJGPP
/* ftp://sunsite.anu.edu.au/pub/pc/simtelnet/gnu/djgpp/v2tk/pmcom10.zip */
#include <../contrib/pmcom/com.h>

char serial;

void serial_open(char *device)
{
  int result;

  serial = atoi(device)-1;
  result = COMPortOpen(serial, (long) 2400, 8, 'N', 2, 0, NULL);

  switch(result) {
  case 0:
    COMSetDtr(serial, 1);
    return;
  case COMERR_NOCHIP:
    printf("tls: COMPortOpen: No UART chip detected for that serial port\n");
    exit(1);
  case COMERR_NOMEMORY:
    printf("tls: COMPortOpen: No memory to allocate receive buffer\n");
    exit(1);
  case COMERR_GENERAL:
    printf("tls: COMPortOpen: Library setup failure\n");
    exit(1);
  default:
    printf("tls: COMPortOpen: unknown status\n");
    exit(1);
  }
}

void serial_halt()
{
  COMSetDtr(serial, 0);
}

void serial_resume()
{
  COMSetDtr(serial, 1);
}

char *serial_read(char *buffer, int size)
{
  char *p, c;
  int l;

  p = buffer;
  l = 0;
  for(;;) {
    int result;

    result = COMReadChar(serial, &c, NULL);
    switch (result) {
    case 0:
      break;
    case COMERR_RXOVERFLOW:
      printf("tls: COMReadChar: receive buffer overflow\n");
      continue;
    case COM_BUFEMPTY:
      continue;
    }

    *p++ = (char) (c & 0xff);
    l++;
    if (c == '\n' || l == size-1) {
      *p++ = '\0';
      return buffer;
    }
  }
}

#else /* TSL_DJGPP */
#include <termios.h>
#include <unistd.h>

FILE *serial;

void serial_open(char *device)
{
  int fd;
  struct termios termios;

  serial = fopen(device, "r" );
  if (serial == NULL) {
    char buffer[1024];

    sprintf(buffer, "fopen: %s", device);
    perror(buffer);
    exit(1);
  }

  /* set the serial port characteristics */
  fd = fileno(serial);
  if (tcgetattr(fd, &termios) < 0) {
    perror("tcgetattr");
    exit(1);
  }

  if (cfsetospeed(&termios, B2400) < 0) {
    perror("cfsetospeed");
    exit(1);
  }

  if (cfsetispeed(&termios, B2400) < 0) {
    perror("cfsetispeed");
    exit(1);
  }

  if (tcsetattr(fd, TCSANOW, &termios) < 0) {
    perror("tcsetattr");
    exit(1);
  }
}

void serial_halt()
{
  /* not required, operating system buffers data */
}

void serial_resume(char *device)
{
  /* not required, operating system buffers data */
}

char *serial_read(char *buffer, int size)
{
  char *p;

  p = fgets(buffer, size, serial);
  if (p == NULL) {
    perror("fgets");
    exit(1);
  }

  return p;
}

#endif /* TSL_DJGPP */

int main ( int argc, char *argv[] )
{
  char *device, *p, buffer[128];
  int i, number, seen[MAXSENSOR];
  time_t now;
  struct tm *tm;
  float datum[MAXSENSOR];
  int gotdata;
  
  gotdata=0;

  /* reset sensor result array */
  for (i=0; i<MAXSENSOR; i++) {
    seen[i] = 0;
    datum[i] = 0.0;
  }

  /* allow user to specify alternate serial port as first argument */
#ifdef TSL_CYGWIN
  device = "COM1";		/* for CYGWIN build */
#else
#ifdef TSL_DJGPP
  device = "1";			/* for DJGPP build */
#else
  device = "/dev/ttyS0";	/* for UNIX build */
#endif
#endif
  if (argc > 1) {
    device = argv[1];
  }

  /* open the serial port, causing DTR to be raised and the sensor to run */
  serial_open(device);
  atexit(serial_halt);

  /* get one set of samples */
  while (!gotdata) {
    /* get a line of data from the sensor */
    p = serial_read(buffer, 128);

    /* remove the terminating lf or cr */
    p = buffer + strlen(buffer) - 1;
    while (*p == '\r' || *p == '\n') {
      *p-- = '\0';
      if (p < buffer) break;
    }

    /* remove preceeding lf or cr */
    p = buffer;
    while (*p == '\r' || *p == '\n') p++;

    /* establish timestamp for this sample */
    now = time(NULL);
    tm = localtime(&now);

    /* check packet header for reset detection */
    if (*p == 'R') {
      continue;
    }

    /* ignore any other packet header other than sensor reports */
    if (!isdigit(*p)) continue;

    /* decode the sensor number */
    number = atoi(p)-1;
    if (number < 0 || number > MAXSENSOR-1) continue;

    /* check for missing blank, ignore the line as corrupt */
    p++;
    if (*p != ' ') continue;

    /* check for verbose mode additional data, and skip it */
    if (strlen(p) > 9) p += 11;

    /* read and decode the data */
    p++;
    sscanf(p, "%f", &datum[number]);

    /* we've seen this sensor, start a countdown */
    seen[number] = 5;

    /* display the current samples */
    for (i=0; i<MAXSENSOR; i++) {
      if (seen[i]) {
	printf("%.2f\t", datum[i]);
	seen[i]--;
      } else {
	printf("X\t");
      }
    }

    /* force display to new line */
    printf("\n");
    gotdata=1;
  }
  return 0;
}
