/*
 * wwsr - Wireless Weather Station Reader
 * 2007 dec 19, Michael Pendec (michael.pendec@gmail.com)
 * Version 0.1
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <signal.h>
#include <ctype.h>
#include <usb.h>

struct usb_dev_handle *devh;
int	ret,mempos=0,showall=0,shownone=0,resetws=0,pdebug=0;
int	o1,o2,o3,o4,o5,o6,o7,o8,o9,o10,o11,o12,o13,o14,o15;
char	buf[1000],*endptr;
char	buf2[400];

void _close_readw() {
    ret = usb_release_interface(devh, 0);
    if (ret!=0) printf("could not release interface: %d\n", ret);
    ret = usb_close(devh);
    if (ret!=0) printf("Error closing interface: %d\n", ret);
}

struct usb_device *find_device(int vendor, int product) {
    struct usb_bus *bus;
    
    for (bus = usb_get_busses(); bus; bus = bus->next) {
	struct usb_device *dev;
	
	for (dev = bus->devices; dev; dev = dev->next) {
	    if (dev->descriptor.idVendor == vendor
		&& dev->descriptor.idProduct == product)
		return dev;
	}
    }
    return NULL;
}

struct tempstat {
	char ebuf[271];
	unsigned short  noffset;
	char delay1;
	char hindoor;
	signed int tindoor;
	unsigned char houtdoor;
	signed int toutdoor;
	unsigned char swind;
	unsigned char swind2;
	unsigned char tempf;
	int pressure;
	unsigned char temph;
	unsigned char tempi;
	signed int rain;
	signed int rain2;
	unsigned char rain1;
	unsigned char oth1;
	unsigned char oth2;
	char nbuf[250];
	char winddirection[100];
} buf4;


void print_bytes(char *bytes, int len) {
    int i;
    if (len > 0) {
	for (i=0; i<len; i++) {
	    printf("%02x ", (int)((unsigned char)bytes[i]));
	}
	// printf("\"");
    }
}
void _open_readw() {
    struct usb_device *dev;
    int vendor, product;
#if 0
    usb_urb *isourb;
    struct timeval isotv;
    char isobuf[32768];
#endif

    usb_init();
//    usb_set_debug(0);
    usb_find_busses();
    usb_find_devices();

    vendor = 0x1941;
    product = 0x8021; 

    dev = find_device(vendor, product);
    assert(dev);
    devh = usb_open(dev);
    assert(devh);
    signal(SIGTERM, _close_readw);
    ret = usb_get_driver_np(devh, 0, buf, sizeof(buf));
    if (ret == 0) {
	// printf("interface 0 already claimed by driver \"%s\", attempting to detach it\n", buf);
	ret = usb_detach_kernel_driver_np(devh, 0);
	// printf("usb_detach_kernel_driver_np returned %d\n", ret);
    }
    ret = usb_claim_interface(devh, 0);
    if (ret != 0) {
	printf("Could not open usb device, errorcode - %d\n", ret);
	exit(1);
    }
    ret = usb_set_altinterface(devh, 0);
    assert(ret >= 0);
}


void _init_wread() {
	char tbuf[1000];
	ret = usb_get_descriptor(devh, 1, 0, tbuf, 0x12);
	// usleep(14*1000);
	ret = usb_get_descriptor(devh, 2, 0, tbuf, 9);
	// usleep(10*1000);
	ret = usb_get_descriptor(devh, 2, 0, tbuf, 0x22);
	// usleep(22*1000);
	ret = usb_release_interface(devh, 0);
	if (ret != 0) printf("failed to release interface before set_configuration: %d\n", ret);
	ret = usb_set_configuration(devh, 1);
	ret = usb_claim_interface(devh, 0);
	if (ret != 0) printf("claim after set_configuration failed with error %d\n", ret);
	ret = usb_set_altinterface(devh, 0);
	// usleep(22*1000);
	ret = usb_control_msg(devh, USB_TYPE_CLASS + USB_RECIP_INTERFACE, 0xa, 0, 0, tbuf, 0, 1000);
	// usleep(4*1000);
	ret = usb_get_descriptor(devh, 0x22, 0, tbuf, 0x74);
}

void _send_usb_msg( char msg1[1],char msg2[1],char msg3[1],char msg4[1],char msg5[1],char msg6[1],char msg7[1],char msg8[1] ) {
	char tbuf[1000];
	tbuf[0] = msg1[0];
	tbuf[1] = msg2[0];
	tbuf[2] = msg3[0];
	tbuf[3] = msg4[0];
	tbuf[4] = msg5[0];
	tbuf[5] = msg6[0];
	tbuf[6] = msg7[0];
	tbuf[7] = msg8[0];
	// print_bytes(tbuf, 8);
	// printf(" - - - \n");
	ret = usb_control_msg(devh, USB_TYPE_CLASS + USB_RECIP_INTERFACE, 9, 0x200, 0, tbuf, 8, 1000);
	// usleep(28*1000);
}

void _read_usb_msg(char *buffer) {
   char tbuf[1000];
   usb_interrupt_read(devh, 0x81, tbuf, 0x20, 1000);
   memcpy(buffer, tbuf, 0x20);
   // usleep(82*1000);
}


void read_arguments(int argc, char **argv) {
	int c,pinfo=0;
	char *mempos1=0,*endptr;
  	shownone=0;
  	o1=0;
	while ((c = getopt (argc, argv, "akwosiurthp:zx")) != -1)
	{
         switch (c)
           {
           case 'a':
  	     showall=1;
	     shownone=1;
             break;
           case 'i':
  	     o1=1;
	     shownone=1;
             break;
           case 'u':
  	     o2=1;
	     shownone=1;
             break;
           case 't':
  	     o3=1;
	     shownone=1;
             break;
           case 'w':
  	     o4=1;
	     shownone=1;
             break;
           case 'r':
  	     o5=1;
	     shownone=1;
             break;
           case 'o':
  	     o6=1;
	     shownone=1;
             break;
           case 's':
  	     o7=1;
	     shownone=1;
             break;
           case 'j':
  	     o9=1;
	     shownone=1;
             break;
           case 'p':
  	     mempos1=optarg;
             break;
           case 'h':
  	     pinfo=1;
             break;
           case 'x':
  	     pdebug=1;
             break;
           case 'z':
  	     resetws=1;
	     shownone=1;
             break;
           case '?':
             if (isprint (optopt))
               fprintf (stderr, "Unknown option `-%c'.\n", optopt);
             else
               fprintf (stderr,"Unknown option character `\\x%x'.\n",optopt);
           default:
             abort ();
           }
	}
	if ( (pinfo!=0) | (shownone==0) ) {
		printf("Wireless Weather Station Reader v0.1\n");
		printf("(C) 2007 Michael Pendec\n\n");
		printf("options\n");
		printf(" -h	help information\n");
		printf(" -p	Start at offset (can be used together with below parameters\n");
		printf(" -x	Show bytes retrieved from device\n");
		printf(" -z	Reset log buffer (will ask for confirmation.\n\n");
		printf(" -a	Show all stats (overrides below parameters)\n");
		printf(" -s	Show current history position\n");
		printf(" -t	Show temperature\n");
		printf(" -j	Show Pressure (hPa)\n");
		printf(" -u	Show humidity\n");
		printf(" -r	Show rain\n");
		printf(" -w	Show wind\n");
		printf(" -o	other \n\n");
		exit(0);
	}
	if (mempos1!=0) {
	    	mempos = strtol(mempos1, &endptr, 16);
	} else {
		printf("Reading last updated record from device\n");
	}
}

int main(int argc, char **argv) {
    int		buftemp;
    char ec='n';
    read_arguments(argc,argv);
    _open_readw();
   _init_wread();

   if (resetws==1) {
     printf(" Resetting WetterStation history\n");
     printf("Sure you want to reset wetter station (y/N)?");
     fflush(stdin);
     scanf("%c",&ec);
     if ( (ec=='y') || (ec=='Y') ) {
   	_send_usb_msg("\xa0","\x00","\x00","\x20","\xa0","\x00","\x00","\x20");
    	_send_usb_msg("\x55","\x55","\xaa","\xff","\xff","\xff","\xff","\xff");
	usleep(28*1000);
     	_send_usb_msg("\xff","\xff","\xff","\xff","\xff","\xff","\xff","\xff");
	usleep(28*1000);
     	_send_usb_msg("\x05","\x20","\x01","\x38","\x11","\x00","\x00","\x00");
	usleep(28*1000);
    	_send_usb_msg("\x00","\x00","\xaa","\x00","\x00","\x00","\x20","\x3e");
	usleep(28*1000);
     } else {
     	printf(" Aborted reset of history buffer\n");
     }
     _close_readw();
     return 0;
   }
   _send_usb_msg("\xa1","\x00","\x00","\x20","\xa1","\x00","\x00","\x20");
   _read_usb_msg(buf2);
   _send_usb_msg("\xa1","\x00","\x20","\x20","\xa1","\x00","\x20","\x20");
   _read_usb_msg(buf2+32);
   _send_usb_msg("\xa1","\x00","\x40","\x20","\xa1","\x00","\x40","\x20");
   _read_usb_msg(buf2+64);
   _send_usb_msg("\xa1","\x00","\x60","\x20","\xa1","\x00","\x60","\x20");
   _read_usb_msg(buf2+96);
   _send_usb_msg("\xa1","\x00","\x80","\x20","\xa1","\x00","\x80","\x20");
   _read_usb_msg(buf2+128);
   _send_usb_msg("\xa1","\x00","\xa0","\x20","\xa1","\x00","\xa0","\x20");
   _read_usb_msg(buf2+160);
   _send_usb_msg("\xa1","\x00","\xc0","\x20","\xa1","\x00","\xc0","\x20");
   _read_usb_msg(buf2+192);
   _send_usb_msg("\xa1","\x00","\xe0","\x20","\xa1","\x00","\xe0","\x20");
   _read_usb_msg(buf2+224);


 //  buf4.noffset = (unsigned char) buf2[22] + ( 256 * buf2[23] );
   buf4.noffset = (unsigned char) buf2[30] + ( 256 * buf2[31] );
   if (mempos!=0) buf4.noffset = mempos;
   buftemp = 0;
   if (buf4.noffset!=0) buftemp = buf4.noffset - 0x10;
   buf[1] = ( buftemp >>8 & 0xFF ) ;
   buf[2] = buftemp & 0xFF;
   buf[3] = ( buftemp >>8 & 0xFF ) ;
   buf[4] = buftemp & 0xFF;
   _send_usb_msg("\xa1",buf+1,buf+2,"\x20","\xa1",buf+3,buf+4,"\x20");
   _read_usb_msg(buf2+224);

ret = usb_control_msg(devh, USB_TYPE_CLASS + USB_RECIP_INTERFACE, 0x0000009, 0x0000200, 0x0000000, buf, 0x0000008, 1000);
// usleep(8*1000);
ret = usb_interrupt_read(devh, 0x00000081, buf, 0x0000020, 1000);
memcpy(buf2+256, buf, 0x0000020);
if ( ( pdebug==1) ) printf("bytes received from device details:\n");
if ( ( pdebug==1) ) print_bytes(buf2+272, 16);
if ( ( pdebug==1) ) printf("\n");
buf4.delay1 = buf2[272];
buf4.tempi=buf2[284];
	if (buf4.tempi==0) strcpy(buf4.winddirection,"N");
	if (buf4.tempi==1) strcpy(buf4.winddirection,"NNE");
	if (buf4.tempi==2) strcpy(buf4.winddirection,"NE");
	if (buf4.tempi==3) strcpy(buf4.winddirection,"ENE");
	if (buf4.tempi==4) strcpy(buf4.winddirection,"E");
	if (buf4.tempi==5) strcpy(buf4.winddirection,"SEE");
	if (buf4.tempi==6) strcpy(buf4.winddirection,"SE");
	if (buf4.tempi==7) strcpy(buf4.winddirection,"SSE");
	if (buf4.tempi==8) strcpy(buf4.winddirection,"S");
	if (buf4.tempi==9) strcpy(buf4.winddirection,"SSW");
	if (buf4.tempi==10) strcpy(buf4.winddirection,"SW");
	if (buf4.tempi==11) strcpy(buf4.winddirection,"SWW");
	if (buf4.tempi==12) strcpy(buf4.winddirection,"W");
	if (buf4.tempi==13) strcpy(buf4.winddirection,"NWW");
	if (buf4.tempi==14) strcpy(buf4.winddirection,"NW");
	if (buf4.tempi==15) strcpy(buf4.winddirection,"NNW");
buf4.hindoor = buf2[273];
buf4.tindoor =( (unsigned char) buf2[274] + (unsigned char) buf2[275] *256);
buf4.houtdoor = buf2[276];
// Check sign of temperature
if (buf2[278]&0x80) {
  buf4.toutdoor = -( (unsigned char) buf2[277] );
} else {
  buf4.toutdoor =( (unsigned char) buf2[277] + (unsigned char) buf2[278] *256);
}
buf4.pressure = (unsigned char) buf2[279] + ( 256*buf2[280]);
buf4.swind = buf2[281];
buf4.swind2 = buf2[282];
buf4.oth1  = buf2[283];
buf4.rain2 = (unsigned char) buf2[285];
buf4.rain =( (unsigned char) buf2[286] + (unsigned char) buf2[287] *256);
buf4.rain1 = buf2[286];
buf4.oth2 = buf2[287];

printf("\n");
unsigned int remain;
if ( (showall==1) | ( o1==1) ) printf("interval:              %5x\n", buf4.delay1);
if ( (showall==1) | ( o2==1) ) printf("indoor humidity        %5d\n", buf4.hindoor);
if ( (showall==1) | ( o2==1) ) printf("outdoor humidity       %5d\n", buf4.houtdoor);
remain = buf4.tindoor%10;
if ((signed) remain<0) remain = remain * -1;
if ( (showall==1) | ( o3==1) ) printf("indoor temperature     %5d.%d\n", buf4.tindoor / 10 ,remain);
remain = buf4.toutdoor%10;
if ((signed) remain<0) remain = remain * -1;
if ( (showall==1) | ( o3==1) ) {
  if (buf4.toutdoor<0 && buf4.toutdoor/10==0) {
    printf("outdoor temperature      -0.%d\n", remain);
  } else {
    printf("outdoor temperature    %5d.%d\n", buf4.toutdoor / 10 ,remain);
  }
}
remain = buf4.swind%10;
if ( (showall==1) | ( o4==1) ) printf("wind speed             %5d.%d\n", buf4.swind / 10 , remain);
remain = buf4.swind2%10;
if ( (showall==1) | ( o4==1) ) printf("wind gust              %5d.%d\n", buf4.swind2 / 10 , remain);
if ( (showall==1) | ( o4==1) ) printf("wind direction         %5s\n", buf4.winddirection);
remain = buf4.rain%10;
if ( (showall==1) | ( o5==1) ) printf("rain                   %5d.%d\n", buf4.rain / 10 , remain);
remain = (buf4.rain2)%10;
if ( (showall==1) | ( o5==1) ) printf("rain 2                 %5d.%d\n", buf4.rain2 / 10 , remain);
// if ( (showall==1) | ( o5==1) ) printf("rain1                  %5d\n", buf4.rain1);
// if ( (showall==1) | ( o5==1) ) printf("rain2                  %5d\n", buf4.rain2);
if ( (showall==1) | ( o6==1) ) printf("other 1                %5d\n", buf4.oth2);
if ( (showall==1) | ( o6==1) ) printf("other 2                %5d\n", buf4.oth1);
remain = buf4.pressure%10;
if ( (showall==1) | ( o9==1) ) printf("pressure(hPa)          %5d.%d\n", buf4.pressure / 10 , remain);
if ( (showall==1) | ( o7==1) ) printf("Current history pos:   %5x\n", buf4.noffset);
printf("\n");

_close_readw();
return 0;
}
