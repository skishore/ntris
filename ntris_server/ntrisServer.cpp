
// class for building a server or client and connecting to another computer
#include "ntrisServer.h"

int main(int argc, char** argv) {
    initServer(DEFAULTPORT);
    run();
}

void sigchld_handler(int s) {
    while(wait(NULL) > 0);
}

int validPort(int PORT) {
    if ((PORT > 1024) && (PORT < 65536))
        return PORT;
    return DEFAULTPORT;
}

void initServer(int PORT) {
    char* address;
    struct ifreq ifr;

    PORT = validPort(PORT);

    if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
        cout << "Error opening socket" << endl;
        exit(0);
    }   

    my_addr.sin_family = AF_INET;           // host byte order
    my_addr.sin_port = htons(PORT);         // short, network byte order
    my_addr.sin_addr.s_addr = INADDR_ANY;   // automatically fill with my IP
    memset(&(my_addr.sin_zero), '\0', 8);   // zero the rest of the struct

    if (bind(sockfd, (struct sockaddr *)&my_addr, sizeof(struct sockaddr)) == -1) {
        cout << "Error binding port" << endl;
        exit(0);
    } 

    if (listen(sockfd, BACKLOG) == -1) {
        cout << "Error listening" << endl;
        exit(0);
    }

    sa.sa_handler = sigchld_handler; // reap all dead processes
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_RESTART;
    if (sigaction(SIGCHLD, &sa, NULL) == -1) {
        cout << "Error in garbage collection" << endl;
        exit(0);
    }

    ifr.ifr_addr.sa_family = AF_INET;  
    strncpy(ifr.ifr_name, "wlan0", IFNAMSIZ-1);
    // get the network address with ioctl
    ioctl(sockfd, SIOCGIFADDR, &ifr);
    address = inet_ntoa(((struct sockaddr_in *)&ifr.ifr_addr)->sin_addr);
    
    if (fcntl(sockfd, F_SETFL, fcntl(sockfd, F_GETFL) | O_NONBLOCK) == -1) 
            cout << "Error making socket nonblocking with fcntl" << endl;

    timeval t;
    gettimeofday(&t, NULL);
    srand(t.tv_usec);

    loadUsersAndScores();
    seekingGame = "";

    cout << "Server running at IP " << address << ", port " << PORT << endl;
}

void loadUsersAndScores() {
    string line, user;
    ifstream myfile("../data/userList.dat");

    if (myfile.is_open()) {
        // a line of comments, and then the list of users registered
        getline (myfile, line);
        while (!myfile.eof()) {
            getline(myfile, user);
            getline(myfile, line);
            passwords[user] = line;
        }
    } else {
        cout << "Unable to open user list for reading." << endl;
    }
    myfile.close();

    myfile.open("../data/highScores.dat");
    if (myfile.is_open()) {
        // a line of comments before the high scores
        getline (myfile, line);
        for (int i = 0; i < NUMHIGHSCORES; i++) {
            getline (myfile, line);
            highScorers[i] = line;
            getline (myfile, line);
            scores[i] = atoi(line.c_str());
        }
    } else {
        cout << "Unable to open high scores list for reading." << endl;
    }
    myfile.close();
}

void loadUserData(string name) {
    if (clients.count(name) == 0)
        return;

    string line;
    ifstream myfile(("../data/" + name + ".usr").c_str());

    if (myfile.is_open()) {
        getline (myfile, line); 
        for (int i = 0; i < NUMUSERSCORES; i++) {
            getline (myfile, line); 
            clients[name].scores[i] = atoi(line.c_str());
        }

        // two lines of comments before the multiplayer history
        getline (myfile, line);
        getline (myfile, line);
        // the client multiplayer record 
        for (int i = 0; i < 2; i++) {
            getline (myfile, line);
            clients[name].record[i] = atoi(line.c_str());
        }
        for (int i = 0; i < NUMUSERSCORES; i++) {
            getline (myfile, line);
            clients[name].lastGames[i] = line;
        }
    } else {
        cout << "Unable to open user " + name + " data for reading." << endl;
    }
    myfile.close();
}

void addUser(string name, string password) {
    ofstream myfile("../data/userList.dat", ios::out | ios::app);

    if (myfile.is_open()) {
        myfile << name << endl;
        myfile << password << endl;
    } else {
        cout << "Unable to open user list for writing." << endl;
    }
    myfile.close();
    
    updateUserData(name);
    passwords[name] = password;
}

void updateUserData(string name) {
    Client* client = &clients[name];
    ofstream myfile(("../data/" + name + ".usr").c_str());

    if (myfile.is_open()) {
        myfile << "top three:" << endl;
        for (int i = 0; i < NUMUSERSCORES; i++) {
            myfile << client->scores[i] << endl;
        }
        myfile << endl << "multiplayer record:" << endl;
        myfile << client->record[0] << endl << client->record[1] << endl;
        for (int i = 0; i < NUMUSERSCORES; i++) {
            myfile << client->lastGames[i] << endl;
        }
    } else {
        cout << "Unable to open user list for writing." << endl;
    }
    myfile.close();
}

void updateHighScores() {
    ofstream myfile("../data/highScores.dat");

    if (myfile.is_open()) {
        myfile << "high scores:" << endl;
        for (int i = 0; i < NUMHIGHSCORES; i++) {
            myfile << highScorers[i] << endl;
            myfile << scores[i] << endl;
        }
    } else {
        cout << "Unable to open high scores list for writing." << endl;
    }
    myfile.close();
}

void addGameResult(string result) {
    ofstream myfile("../data/scores.dat", ios::out | ios::app);

    if (myfile.is_open()) {
        myfile << result << endl;
    } else {
        cout << "Unable to open user list for writing." << endl;
    }
    myfile.close();
}

void getGameResult(string name, int result, bool multiplayer, string advName, int gamesPlayed) {
    if (clients.count(name) == 0)
        return;
    Client* client = &clients[name];

    if (multiplayer) {
        Client* adv = &clients[advName];

        client->record[0] += result; 
        client->record[1] += gamesPlayed; 
        adv->record[0] += (gamesPlayed - result);
        adv->record[1] += gamesPlayed;
        
        client->curRecord[0] = 0;
        client->curRecord[1] = 0;
        adv->curRecord[0] = 0;
        adv->curRecord[1] = 0;

        for (int i = NUMUSERSCORES-1; i > 0; i--) {
            client->lastGames[i] = client->lastGames[i-1]; 
            adv->lastGames[i] = adv->lastGames[i-1];
        }
        client->lastGames[0] = "vs_" + advName + ":_" + intToString(result) + "/" + intToString(gamesPlayed); 
        adv->lastGames[0] = "vs_" + name + ":_" + intToString(gamesPlayed-result) + "/" + intToString(gamesPlayed); 
        updateUserData(name);
        updateUserData(advName);
    } else {
        if (addScore(client->scores, NUMUSERSCORES, result) > -1)
            updateUserData(name);
        int pos = -1;
        for (int i = 0; i < NUMHIGHSCORES; i++) {
            if (result > scores[i]) {
                pos = i;
                break;
            } else if (name == highScorers[i]) {
                break;
            }
        }
        if (pos >= 0) {
            int oldPos = NUMHIGHSCORES-1;
            for (int i = NUMHIGHSCORES-2; i >= pos; i--) {
                if (name == highScorers[i]) {
                    oldPos = i;
                    break;
                }
            }
            for (int i = oldPos; i > pos; i--) {
                scores[i] = scores[i-1];
                highScorers[i] = highScorers[i-1];
            }
            scores[pos] = result;
            highScorers[pos] = name;
            updateHighScores();
        }
    }
}

int addScore(int* scoreList, int numScores, int newScore) {
    int pos = -1; 

    for (int i = 0; i < numScores; i++) {
        if (newScore > scoreList[i]) {
            pos = i;
            break;
        }
    }
    if (pos >= 0) {
        for (int i = numScores-1; i > pos; i--) {
            scoreList[i] = scoreList[i-1];
        }
        scoreList[pos] = newScore;
    }
    return pos;
}

void run() {
    while (true) {  
        acceptConnections();
        getUsernames();
        routePackets();
        usleep(UPDATEDELAY);
    }
}

void acceptConnections() {
    int portfd;

    // main accept() loop, looking for clients
    sin_size = sizeof(struct sockaddr_in);
    if ((portfd = accept(sockfd, (struct sockaddr*)&their_addr, &sin_size)) == -1) {
        if (errno != EWOULDBLOCK) {
            cout << "Error accepting connection" << endl;
        }   
        return;
    }
    if (fcntl(portfd, F_SETFL, fcntl(portfd, F_GETFL) | O_NONBLOCK) == -1) 
        cout << "Error making socket nonblocking with fcntl" << endl;

    cout << "Received a connection from IP " << inet_ntoa(their_addr.sin_addr) << endl;
    newClients.insert(portfd);
}

void getUsernames() {
    int portfd;
    string logon;
    bool success;
    set<int>::iterator it = newClients.begin();

    // looks through new connections, and assigns them usernames
    while (it != newClients.end()) {
        portfd = *it;
        logon = nextToken(portfd);
        success = false;
        
        if (logon != "") {
            for (int i = 0; i < logon.length(); i++) {
                if (logon[i] == '.')
                    logon[i] = ' ';
            }
            stringstream stream(logon);
            string type, name, password;
            
            stream >> type;
            stream >> name;
            stream >> password;
            if (type == "logon") {
                if ((passwords.count(name) > 0) && (passwords[name] == password)) {
                    if (clients.count(name) == 0) {
                        success = true;
                    } else {
                        sendMessage(portfd, "loggedin");
                        cout << name << " is already logged in." << endl;
                    }
                } else {
                    sendMessage(portfd, "invalidnamepass");
                    cout << name << ", " << password << " is an invalid name/password pair." << endl;
                }
            } else if ((passwords.count(name) == 0) && (name != "me")) {
                success = true;
            } else {
                sendMessage(portfd, "taken");
                cout << name << " is taken." << endl;
            }

            if (success) {
                sendMessage(portfd, "getname." + name + userList());
                clients[name] = Client(portfd);
                if (type == "logon") {
                    loadUserData(name);
                } else {
                    addUser(name, password);
                }
                names[portfd] = name;
                newClients.erase(it++);
                sendUpdate("logon." + name);
            } else {
                it++;
            }
        } else {
            it++;
        }
    }
}

void routePackets() {
    int portfd;
    string name;
    string token;
    map<string,Client>::iterator it = clients.begin();

    // routes packets sent between clients who have already signed in
    while(it != clients.end()) {
        name = (*it).first;
        portfd = (*it).second.portfd;
        it++;

        if (clients[name].timeout-- < 0) {
            sendMessage(portfd, "booted");
            lostConnection(portfd);
        } else {
            token = nextToken(portfd);
            while (token != "") {
                handleCommand(token, portfd);
                token = nextToken(portfd);
            }
        }
    }
}

void sendMessage(int portfd, string message) {
    message += "\n";
    const char* buf = message.c_str();
   
    if (send(portfd, buf, strlen(buf), MSG_NOSIGNAL) == -1) {
        cout << "Error sending message " << message;
        lostConnection(portfd);
    }
}

string recvMessage(int portfd, bool peek=false) {
    int numbytes;
    char* buf = new char[MAXDATASIZE];

    int flags = 0;
    if (peek == true)
        flags = MSG_PEEK;

    if ((numbytes = recv(portfd, buf, MAXDATASIZE-1, flags)) == -1) {
        if (errno != EWOULDBLOCK) {
            lostConnection(portfd);
        }
        delete buf;
        return "";
    }

    string ans(buf, numbytes);
    delete buf;
    return ans;
}

string nextToken(int portfd) {
    string word;
    stringstream stream(recvMessage(portfd, true));
    stream >> word;
 
    int size = word.length() + 1;

    if (word != "") {
        char* buf = new char[size];
        if (recv(portfd, buf, size, 0) == -1)
            if (errno != EWOULDBLOCK) {
                cout << "Error in recv()" << endl;
            } 
        delete buf;
    }
    
    return word;
}

void handleCommand(string command, int portfd) {
    string type, part, name;
    if (names.count(portfd) == 0)
        return;
    name = names[portfd];

    clients[name].timeout = TIMEOUT;

    for (int i = 0; i < command.length(); i++) {
        if (command[i] == '.')
            command[i] = ' ';
    }
    stringstream stream(command);

    stream >> type;
    if (type == "checkin") {
        return;
    } else if (type == "ntrisEvent") {
        for (int i = 0; i < command.length(); i++) {
            if (command[i] == ' ')
            command[i] = '.';
        }
        if (clients.count(clients[name].adv) > 0)
            sendMessage(clients[clients[name].adv].portfd, command);
    } else if (type == "ntris") {
        string adv;

        if (clients[name].adv != "") {
            return;
        } else if (stream >> part) {
            if ((part != name) and (clients.count(part) > 0)) {
                if ((part != seekingGame) and (clients[part].invite != name)) {
                    string oldInvite = clients[name].invite;
                    clients[name].invite = part;
                    sendUpdate("invite." + name + "." + part);
                    return;
                } else {
                    clients[part].invite = "";
                    adv = part;
                }
            }
        } else {
            for (map<string,Client>::iterator it = clients.begin(); it != clients.end(); it++) {
                if ((*it).second.invite == name) {
                    (*it).second.invite = "";
                    adv = (*it).first;
                }
            }
        }
        
        if ((adv == "") and (seekingGame != "") and (seekingGame != name)) {
            adv = seekingGame;
            seekingGame = "";
        }
        
        if (adv == "") {
            seekingGame = name;
            sendUpdate("seekingGame." + name);
        } else {
            if ((adv == seekingGame) || (name == seekingGame))
                seekingGame = "";

            clients[name].adv = adv;
            clients[name].invite = "";
            clients[adv].adv = name;
            clients[adv].invite = "";

            clients[name].curRecord[0] = 0;
            clients[name].curRecord[1] = 0;
            clients[adv].curRecord[0] = 0;
            clients[adv].curRecord[1] = 0;

            string seed = intToString(rand());
            sendUpdate("ntris." + adv + "." + name + "." + seed);
            sendUpdate("ntris." + name + "." + adv + "." + seed);
        }
    } else if (type == "talk") {
        stream >> part;
        sendUpdate("talk." + name + "." + part);
    } else if (type == "cancel") {
        if (seekingGame == name)
            seekingGame = "";
        clients[name].invite = "";
        sendUpdate("cancel." + name);
    } else if (type == "record") {
        Client* client = &clients[name];
        string ans = "record.YOUR_TOP_THREE_GAMES:.";

        for (int i = 0; i < NUMUSERSCORES; i++) {
            if (client->scores[i] >= 0) 
                ans += intToString(client->scores[i]);
            ans += ".";
        }
        ans += ".MULTIPLAYER_RECORD:_won_";
        ans += intToString(client->record[0]) + "/" + intToString(client->record[1]) + ".";
        ans += ".LAST_THREE_MULTIPLAYER_GAMES:.";
        for (int i = 0; i < NUMUSERSCORES; i++) 
            ans += client->lastGames[i] + ".";

        ans += ".....OVERALL_TOP_TEN:";
        for (int i = 0; i < NUMHIGHSCORES; i++) {
            ans += (i < 9 ? ". " : ".") + intToString(i+1) + ")_";
            if (scores[i] >= 0) 
                ans += highScorers[i] + "_" + intToString(scores[i]);
        }

        sendMessage(portfd, ans);
    } else if (type == "result") {
        addGameResult(name + " " + command);
        stream >> part;
        if (part == "singleplayer") {
            stream >> part;
            int result = atoi(part.c_str());
            getGameResult(name, result, false);
        } else {
            stream >> part;
            if (part != "0")
                clients[name].curRecord[0]++;
            clients[name].curRecord[1]++;
        }
    } else if (type == "quit") {
        string oldAdv = clients[name].adv;
        if (clients.count(oldAdv) > 0) {
            stream >> part;
            if (part == "ingame")
                clients[name].curRecord[1]++;
            getGameResult(name, clients[name].curRecord[0], true, oldAdv, clients[name].curRecord[1]);

            clients[oldAdv].adv = "";
        }
        clients[name].adv = "";
        sendUpdate("quit." + name + "." + oldAdv);
    } else if (type == "logoff") {
        lostConnection(portfd);
    }
}

string intToString(int num) {
    stringstream stream;
    string ans;

    stream << num;
    stream >> ans;
    return ans;
}

void lostConnection(int portfd) {
    if (names.count(portfd) > 0) {
        string name = names[portfd];
        if (seekingGame == name) 
            seekingGame = "";
        if (clients.count(clients[name].adv) > 0) {
            getGameResult(name, clients[name].curRecord[0], true, 
                    clients[name].adv, clients[name].curRecord[1]);
            clients[clients[name].adv].adv = "";
        }
        clients.erase(name);
        names.erase(portfd);
        close(portfd);
        sendUpdate("logoff." + name);
    }
}

void sendUpdate(string message) {
    map<string,Client>::iterator it = clients.begin();
        
    cout << message << endl; 

    while (it != clients.end()) {
        int portfd = (*it).second.portfd;
        it++;
        sendMessage(portfd, message);
    }
}

string userList() {
    string users = "";
    string nextUser;

    for (map<string,Client>::iterator it = clients.begin(); it != clients.end(); it++) {
        nextUser = (*it).first;
        if (nextUser == seekingGame) {
            nextUser += " is seeking a game";
        } else if (clients[nextUser].adv != "") {
            nextUser += " (playing " + clients[nextUser].adv + ")";
        }
        users += "." + nextUser;
    }
    return users;
}

void closeConnections() {
    close(sockfd);
    cout << "Connections closed." << endl;
}

