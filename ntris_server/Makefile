TARGETS=	Gen/server

PROJECT=	./

CXXFLAGS=	-g -O2 

LDFLAGS=	

SOURCES=	ntrisServer.cpp

ifneq ($(INCLUDE_DEPENDENCIES),yes)

all:	
	@make --no-print-directory INCLUDE_DEPENDENCIES=yes $(TARGETS)
	
.PHONY:	clean
clean:
	rm -rf Gen

else

-include $(addprefix Gen/,$(SOURCES:.cpp=.d))
-include $(addprefix Gen/,$(SOURCES:.cpp=.d))

endif

FLOAT_OBJECTS= $(addprefix Gen/,$(SOURCES:.cpp=.o))

.INTERMEDIATE: $(FLOAT_OBJECTS)

Gen/%.o:		%.cpp
	mkdir -p $(dir $@)
	c++ $(CXXFLAGS) -c -o $@ $<

Gen/%.o:		%.c
	mkdir -p $(dir $@)
	cc $(CXXFLAGS) -c -o $@ $<

Gen/server:	$(FLOAT_OBJECTS)
	g++ -o $@ $^ $(LDFLAGS)

Gen/%.d:		%.cpp
	@mkdir -p $(dir $@)
	c++ -M -MT $(@:.d=.o) $(CXXFLAGS) -o $@ $<

