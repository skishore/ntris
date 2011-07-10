
using namespace std;

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/wait.h>
#include <signal.h>
#include <sys/ioctl.h>
#include <sys/fcntl.h>
#include <net/if.h>

#include <set>
#include <map>
#include <iostream>
#include <fstream>
#include <cstring>
#include <complex>
#include "sys/time.h"

// the port server and client will connect to
#define DEFAULTPORT 1618
// the maximum number of bytes sent in a single packet
#define MAXDATASIZE 64
// how many pending connections the queue will hold
#define BACKLOG 8 
// the number of server updates per second
#define UPDATESPERSECOND 30
#define TICKS_PER_SEC 1000000
#define UPDATEDELAY (TICKS_PER_SEC/UPDATESPERSECOND)
// how many updates a client can avoid checking in before we boot him
#define TIMEOUT 8*UPDATESPERSECOND

// the number of places on the overall and single user high-score lists
#define NUMHIGHSCORES 10
#define NUMUSERSCORES 3

// connected becomes true once a client finds a server or vice versa
bool connected = false;
// listen on the socket sockfd, and add the new connection on newfd
int sockfd;
// my address's information
struct sockaddr_in my_addr;
// connector's address's information
struct sockaddr_in their_addr;
// size of our socket in memory
socklen_t sin_size;
// variable for garbage collection
struct sigaction sa;

struct Client {
    int portfd;
    int timeout;
    string invite;
    string adv;
    int scores[NUMUSERSCORES];
    string lastGames[NUMUSERSCORES];
    int record[2];
    int curRecord[2];

    Client(int port=0) {
        portfd = port;
        timeout = TIMEOUT;
        invite = "";
        adv = "";

        for (int i = 0; i < NUMUSERSCORES; i++) {
            scores[i] = -1;
            lastGames[i] = "";
        }
        record[0] = 0;
        record[1] = 0;
        curRecord[0] = 0;
        curRecord[1] = 0;
    }
};

// list of clients connected, and list of new (as yet unnamed) connections
// also a list of games being played: a single player's opponent is ""
set<int> newClients;
map<string, Client> clients;
map<int, string> names;
string seekingGame;

// information for user histories: passwords and the overall high scores list
map<string, string> passwords;
string highScorers[NUMHIGHSCORES];
int scores[NUMHIGHSCORES];
 
struct IPv4 {
    unsigned char b1, b2, b3, b4;
};

// method run for cleanup
void sigchld_handler(int);
// set up a server, specifying a port
void initServer(int);
// check if port is valid - return default port otherwise
int validPort(int);
// methods for accepting connections and routing packets, called by run()
void run();
void acceptConnections();
void getUsernames();
void routePackets();
// send across the connection
void sendMessage(int, string);
// receive from the connection
string recvMessage(int, bool);
// pull the next packet from the connection - packets are " " delimited
string nextToken(int);
// process a command from a client
void handleCommand(string, int);
// convert an integer to a string
string intToString(int);
// called when we lose a connection on a port
void lostConnection(int);
// called to update clients on who is playing a game
void sendUpdate(string);
// prints the set of users at the server
string userList();
// close all open sockets
void closeConnections();

// load the list of registered users, add a new user, and update a user's history
void loadUsersAndScores();
void loadUserData(string);
void addUser(string, string);
void updateUserData(string);
void updateHighScores();
// receive the result of a game and update files as needed
void getGameResult(string, int, bool, string="", int=0);
int addScore(int*, int, int);

