import json, uuid, pprint
from datauri import DataURI
from random import randint, choice, shuffle
from threading import Lock
from flask import Flask, render_template, session, request, \
    copy_current_request_context
from flask_socketio import SocketIO, emit, join_room, leave_room, \
    close_room, rooms, disconnect


cadaverGames = {}
pp = pprint.PrettyPrinter(indent=4)

def generateDrawings(numPlayers):
    drawings = []
    for n in range(numPlayers):
        canvaslist = []
        for i in range(numPlayers):
            canvaslist.append(str(uuid.uuid4()))
        drawings.append(canvaslist)
    #print(drawings)
    return drawings

def generateAvatars(multiples=5):
    shape = ["S"+str(i) for i in range(13)]
    color = ["c"+str(i) for i in range(10)]
    animation = ["A"+str(i) for i in range(7)]


    if multiples < 1:
        multiples = 1
    shapes = []
    colors = []
    animations = []
    for i in range(multiples):
        shuffle(shape)
        shapes+=shape[:]
        shuffle(color)
        colors+=color[:]
        shuffle(animation)
        animations+=animation[:]

    avatars = list(zip(shapes, colors, animations))
    #print(avatars)
    return avatars

def loadFromServer(room):
    with open(room, 'rb') as f:
        r = f.read()
        
        return json.loads(r)


class CadaverGame:

    def __init__(self, room, canvasWidth, URL):

        self.room = room
        self.canvasWidth = canvasWidth
        self.URL = URL

        self.players = []
        self.waitTurnForPlayers = []
        self.canvas = {}
        self.canvasTurns = {}
        self.activeCanvasTurn = 0
        self.isLastCanvasTurn = False
        self.drawings = []
        self.avatars = generateAvatars()
        self.avatarindex = 0
        self.status = "waiting"

    def joinGame(self, playerId, playerName, isAdmin):
        p = {"playerId": playerId, "name": playerName, "avatar": self.avatars[self.avatarindex], "isAdmin": isAdmin}
        self.players.append(p)
        self.avatarindex += 1

    def hasPlayer(self, playerId):

        pExists = False
        for p in self.players:
            if p['playerId'] == playerId:
                pExists = True
                break

        return pExists

    def leaveGame(self, playerId):

        pToLeave = None

        for p in self.players:
            if p['playerId'] == playerId:
                pToLeave = p

        if pToLeave:
            self.players.remove(pToLeave)
            if pToLeave['isAdmin']:
                newAdmin = choice(self.players)
                self.giveAdmin(newAdmin['playerId'])

    def assignCanvastoPlayerTurns(self):

        # This canvas-player-turn assignment is not immediately trivial
        # For n players, we have n turns with n canvas
        # Each player gets one ordered canvas per turn so that
        # they get distinct canvas position per drawing per turn
        # For instance. A 3 player game will get player 1 with
        # first canvas on first drawing, 2nd on 2nd drawing and 3rd on 3rd


        drawings = self.drawings
        size = len(self.players)

        canvas_list = [canvas for sublist in drawings for canvas in sublist]

        pos = range(size)
        canvas_turn_dict = {}

        for p in pos:
            playerID = self.players[p]["playerId"]
            l = {}
            for i in range(size):
                index = (i*(size+1)+size*p)%len(canvas_list)
                c = canvas_list[index]
                if i==0:
                    prev_c = None
                else:
                    index = (i*(size+1)+size*p)%len(canvas_list)-1
                    prev_c = canvas_list[index]
                l[i] = (c,prev_c)
            canvas_turn_dict[playerID] = l

        return canvas_turn_dict



    def startGame(self):

        turnsdict = {}
        self.drawings = generateDrawings(len(self.players))
        self.canvas = {uid:None for sublist in self.drawings for uid in sublist}
        self.waitTurnForPlayers = [p["playerId"] for p in self.players]
        if len(self.waitTurnForPlayers) == 1:
            self.isLastCanvasTurn = True
        count = 0

        self.canvasTurns = self.assignCanvastoPlayerTurns()
        self.status = "ongoing"

    def receiveTurnFromPlayer(self, playerId, canvasId, canvasDataURI, canvasWidth, canvasHeight):
        if playerId in self.waitTurnForPlayers and canvasDataURI:
            self.canvas[canvasId] = {"dataURI": canvasDataURI, "width": canvasWidth, "height": canvasHeight}
            self.waitTurnForPlayers.remove(playerId)
        print("waiting list", self.waitTurnForPlayers)

    def allCanvasAreIn(self):
        return (len(self.waitTurnForPlayers) == 0)

    def nextTurn(self):

        if not self.isLastCanvasTurn:
            self.activeCanvasTurn +=1
            if len(self.players) == self.activeCanvasTurn + 1:
                self.isLastCanvasTurn = True
        self.waitTurnForPlayers = [p["playerId"] for p in self.players]


    def endGame(self):

        self.status = "finished"


    def buildAvatars(self):
        self.avatars = generateAvatars()

    def isAdmin(self, playerId):

        for p in self.players:
            if p['playerId'] == playerId and p['isAdmin'] == True:
                return True

        return False

    def giveAdmin(self, playerId):

        pToAdmin = None

        for p in self.players:
            if p['playerId'] == playerId:
                pToAdmin = p
                break

        if pToAdmin:
            pToAdmin['isAdmin'] = True


    def _repr_(self):
        return json.dumps(dir(self))

    def toJSON(self):
        return self.__dict__
        # return json.dumps(self, default=lambda o: o.__dict__,
        #     sort_keys=True, indent=4)

    def saveToServer(self):
        with open(self.room, 'w') as f:
            f.write(self.toJSON())






# Set this variable to "threading", "eventlet" or "gevent" to test the
# different async modes, or leave it set to None for the application to choose
# the best option based on installed packages.
async_mode = "eventlet"

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 1024*1024*50
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, async_mode=async_mode, manage_session=True, max_http_buffer_size=1024*1024*50)
thread = None
thread_lock = Lock()

app.cadaverGames = cadaverGames


def background_thread():
    """Example of how to send server generated events to clients."""
    count = 0
    while True:
        socketio.sleep(10)
        count += 1
        socketio.emit('my_response',
            {'data': 'Server generated event', 'count': count,})


@app.route('/')
def index():
    return render_template('index.html', async_mode=socketio.async_mode)



@socketio.event
def joinGame(message):

    response = {}
    room = message['room']
    playerID = session["playerID"]
    playerName = message['name']

    join_room(room) # socket room

    session['receive_count'] = session.get('receive_count', 0) + 1
    session["room"] = room

    response.update({'count': session['receive_count']})
    response.update({'origin': 'joinGame'})
    response.update({'info': f'{playerID} joined game: ' + room})

    with app.app_context():

        if room not in app.cadaverGames.keys():
            game = CadaverGame(room,2048,'https://')
            game.joinGame(playerID, playerName, True)
            app.cadaverGames[room] = game

        else:
            if not app.cadaverGames[room].hasPlayer(playerID):
                app.cadaverGames[room].joinGame(playerID, playerName, False)
                if len(app.cadaverGames[room].players) == 1:
                    app.cadaverGames[room].giveAdmin[playerID]

        game = app.cadaverGames[room]
        response.update({'data': game.toJSON()})

        #print(game.toJSON())

    print("payload", response)

    emit('payload', response, to=room)


@socketio.event
def startGame(message):

    room = session["room"]
    playerID = session["playerID"]
    response = {}

    response.update({'count': session['receive_count']})
    response.update({'origin':'startGame'})
    response.update({'info': 'Game has started!'})

    with app.app_context():

        #TODO check for existing room, raise error otherwise

        game = app.cadaverGames[room]
        if game.isAdmin(playerID):
            game.startGame()
            response.update({'data': game.toJSON()})

        #print(game.toJSON())

            emit('payload', response, to=room)

@socketio.event
def collectCanvas(message):

    room = session["room"]
    playerID = session["playerID"]
    response = {}

    response.update({'count': session['receive_count']})
    response.update({'origin':'collectCanvas'})
    response.update({'info': 'End of turn! Time to collect canvas'})

    with app.app_context():
        game = app.cadaverGames[room]
        if game.isAdmin(playerID):

            emit('collectCanvas', response, to=room)

@socketio.event
def endGame(message):

    room = session["room"]
    playerID = session['playerID']
    response = {}

    with app.app_context():
        game = app.cadaverGames[room]
        response.update({'count': session['receive_count']})
        response.update({'origin':'endGame'})
        response.update({'info': 'End Game!'})

        response.update({'data': game.toJSON()})

    emit('payload', response, to=room)

@socketio.event
def sendCanvas(message):

    room = session["room"]
    playerID = session['playerID']
    dataURI = message['dataURI']
    canvasWidth = message['canvasWidth']
    canvasHeight = message['canvasHeight']
    print(message)

    session['receive_count'] = session.get('receive_count', 0) + 1
    response = {}
    response.update({'count': session['receive_count']})
    response.update({'origin':'sendCanvas'})

    with app.app_context():
        game = app.cadaverGames[room]
        canvasId = game.canvasTurns[playerID][game.activeCanvasTurn][0]
        game.receiveTurnFromPlayer(playerID, canvasId, dataURI, canvasWidth, canvasHeight)

        if game.allCanvasAreIn() and not game.isLastCanvasTurn: #final canvas on a non final turn
            response.update({'info': 'Yours was the last canvas!'})
            print('Yours was the last canvas!')
            game.nextTurn()
            nextTurn(message)
        elif game.allCanvasAreIn() and game.isLastCanvasTurn: #final canvas on a final turn
            response.update({'info': 'Yours was the last canvas of the last turn!'})
            print('Yours was the last canvas of the last turn!')
            game.endGame()
            endGame(message)
        else: #any non final canvas on any turn
            response.update({'info': 'Your canvas has been sent! Still waiting for the rest of players'})
            print('Your canvas has been sent! Still waiting for the rest of players')
            emit('sendCanvas', response)





@socketio.event
def nextTurn(message):

    room = session["room"]
    playerID = session['playerID']
    response = {}

    response.update({'count': session['receive_count']})
    response.update({'origin':'nextTurn'})
    response.update({'info': 'Next turn!'})

    with app.app_context():

        #TODO check for existing room, raise error otherwise
        game = app.cadaverGames[room]
        if game.hasPlayer(playerID) and game.isAdmin(playerID):

            game.nextTurn()

        response.update({'data': game.toJSON()})


        pp.pprint(game.toJSON())
    print("It's time for a new turn!")
    emit('payload', response, to=room)




@socketio.event
def leaveGame(message):

    response = {}
    room = session["room"]
    playerID = session["playerID"]

    session['receive_count'] = session.get('receive_count', 0) + 1
    response.update({'count': session['receive_count']})
    response.update({'origin':'leaveGame'})
    response.update({'info':f'{playerID} left the room '+room})

    with app.app_context():
        game = app.cadaverGames[room]
        response.update({'data': game.toJSON()})
        game.leaveGame(playerID)

    leave_room(room) # socket room

    emit('payload', response, to=room)


@socketio.event
def disconnect_request():
    @copy_current_request_context
    def can_disconnect():
        disconnect()

    response = {}
    session['receive_count'] = session.get('receive_count', 0) + 1
    response.update({'count': session['receive_count']})
    response.update({'data': 'Disconnected!'})
    response.update({'type': 'disconnect_request'})
    # for this emit we use a callback function
    # when the callback function is invoked we know that the message has been
    # received and it is safe to disconnect
    emit('my_response', response, callback=can_disconnect)


# request del payload con room en request para recuperar en el futuro

@socketio.event
def payload(message):

    room = session["room"]
    response = {}

    with app.app_context():
        game = app.cadaverGames[room]
        response.update({'origin':'payload'})
        response.update({'count': session['receive_count']})
        response.update({'data': game.toJSON()})
        response.update({'info':'Payload for room '+room})
        response.update({'callbackId': callbackId})

    emit('payload', response)



@socketio.event
def saveToServer(message):

    room = session["room"]
    response = {}

    with app.app_context():
        game = app.cadaverGames[room]
        response.update({'count': session['receive_count']})
        response.update({'origin':'saveToServer'})
        response.update({'info': 'Game payload saved'})

    emit('saveToServer', response)


@socketio.event
def loadFromServer(message):

    room = message['room']
    playerID = session['playerID']
    response = {}

    with app.app_context():
        game = app.cadaverGames[room]
        response.update({'count': session['receive_count']})
        response.update({'origin':'loadFromServer'})
        response.update({'info': 'Old room payload'})

        response.update({'data': loadFromServer(room)})

    emit('payload', response)

@socketio.event
def connect(message):

    global thread
    with thread_lock:
        if thread is None:
            thread = socketio.start_background_task(background_thread)

    playerID = request.args.get('playerID')

    session["playerID"] = playerID

    response = {}
    room = None

    #print("socketio",playerID)

    with app.app_context():
        for k, v in app.cadaverGames.items():
            if v.hasPlayer(playerID):
                print(f"Welcome back to your {k} room!",playerID)
                join_room(k)

                game = app.cadaverGames[k]
                session["receive_count"] = 0
                session["room"] = k
                room = k
                response.update({'data': game.toJSON()})

    emit("payload", response, to=room)


@socketio.on('disconnect')
def test_disconnect():
    print('Client disconnected')


if __name__ == '__main__':
    socketio.run(app)
