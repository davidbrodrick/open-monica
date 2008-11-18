all: wwsr tsl

wwsr: wwsr.c Makefile
	gcc -lusb -o wwsr wwsr.c

tsl: tsl.c Makefile
	gcc -o tsl tsl.c

clean:
	rm -f wwsr tsl
