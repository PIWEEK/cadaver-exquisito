import json, uuid
from datauri import DataURI
from random import randint, choice, shuffle
from threading import Lock
from flask import Flask, render_template, session, request, \
    copy_current_request_context
from flask_socketio import SocketIO, emit, join_room, leave_room, \
    close_room, rooms, disconnect


cadaverGames = {}        

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

    def joinGame(self, playerId, name, isAdmin):

        p = {"playerId": playerId, "name": name, "avatar": self.avatars[self.avatarindex], "isAdmin": isAdmin}
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


    def startGame(self):

        turnsdict = {}
        self.drawings = generateDrawings(len(self.players))
        self.canvas = {uid:None for sublist in self.drawings for uid in sublist}
        self.waitTurnForPlayers = [p["playerId"] for p in self.players]
        if len(self.waitTurnForPlayers) == 1:
            self.isLastCanvasTurn = True
        count = 0

        for p in self.players:
            pdict = {}
            turn = 0
            for i in self.players:
                if turn == 0:
                    pdict.update({turn: [self.drawings[count][turn],None]})
                else:
                    pdict.update({turn: [self.drawings[count][turn],self.drawings[count][turn-1]]})
                turn +=1 
            count += 1

            turnsdict.update({p["playerId"]: pdict})
        
        self.canvasTurns = turnsdict
        self.status = "ongoing"
        
    def receiveTurnFromPlayer(self, playerId, canvasId, canvasDataURI):
        if playerId in self.waitTurnForPlayers and canvasDataURI:
            self.canvas[canvasId] = canvasDataURI
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
        return json.dumps(self, default=lambda o: o.__dict__, 
            sort_keys=True, indent=4)


# Set this variable to "threading", "eventlet" or "gevent" to test the
# different async modes, or leave it set to None for the application to choose
# the best option based on installed packages.
async_mode = None

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, async_mode=async_mode, manage_session=True)
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
    tabID = message['tabID']

    join_room(room) # socket room

    session['receive_count'] = session.get('receive_count', 0) + 1

    response.update({'count': session['receive_count']})
    response.update({'origin': 'joinGame'})
    response.update({'info': f'{tabID} joined game: ' + room})
    
    with app.app_context():

        if room not in app.cadaverGames.keys():
            game = CadaverGame(room,2048,'https://')
            game.joinGame(tabID, tabID, True)
            app.cadaverGames[room] = game

        else:
            if not app.cadaverGames[room].hasPlayer(tabID):
                app.cadaverGames[room].joinGame(tabID, tabID, False)
                if len(app.cadaverGames[room].players) == 1:
                    app.cadaverGames[room].giveAdmin[tabID]

        game = app.cadaverGames[room]
        response.update({'data': game.toJSON()})
        
        #print(game.toJSON())

    emit('payload', response, to=room)


@socketio.event
def startGame(message):

    room = message['room']
    response = {}

    response.update({'count': session['receive_count']})
    response.update({'origin':'startGame'})
    response.update({'info': 'Game has started!'})

    with app.app_context():

        #TODO check for existing room, raise error otherwise

        game = app.cadaverGames[room]
        game.startGame()
        response.update({'data': game.toJSON()})
    
        #print(game.toJSON())

    emit('payload', response, to=room)

@socketio.event
def endTurn(message):

    room = message['room']
    tabID = message['tabID']
    response = {}

    response.update({'count': session['receive_count']})
    response.update({'origin':'endTurn'})
    response.update({'info': 'End turn!'})


    emit('endTurn', response, to=room)

@socketio.event
def endGame(message):

    room = message['room']
    tabID = message['tabID']
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

    room = message['room']
    tabID = message['tabID']
    dataURI = message['dataURI']
    print(message)
    
    session['receive_count'] = session.get('receive_count', 0) + 1
    response = {}
    response.update({'count': session['receive_count']})
    response.update({'origin':'sendCanvas'})

    with app.app_context():
        game = app.cadaverGames[room]
        canvasId = game.canvasTurns[tabID][game.activeCanvasTurn][0]
        game.receiveTurnFromPlayer(tabID, canvasId, dataURI)

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

    room = message['room']
    tabID = message['tabID']
    response = {}

    response.update({'count': session['receive_count']})
    response.update({'origin':'nextTurn'})
    response.update({'info': 'Next turn!'})

    with app.app_context():

        #TODO check for existing room, raise error otherwise
        game = app.cadaverGames[room]
        if game.hasPlayer(tabID) and game.isAdmin(tabID):
            
            game.nextTurn()

        response.update({'data': game.toJSON()})
    
        #print(game.toJSON())
    print("It's time for a new turn!")
    emit('payload', response, to=room)




@socketio.event
def leaveGame(message):

    response = {}
    room = message['room']
    tabID = message['tabID']
    try:
        callbackId = message['callbackId']
    except:
        callbackId = ""

        session['receive_count'] = session.get('receive_count', 0) + 1
        response.update({'count': session['receive_count']})
        response.update({'origin':'leaveGame'})
        response.update({'info':f'{tabID} left the room '+room})

    with app.app_context():
        game = app.cadaverGames[room]
        response.update({'data': game.toJSON()})
        game.leaveGame(tabID)

    leave_room(message['room']) # socket room

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

    room = message['room']
    response = {}
    try:
        callbackId = message['callbackId']
    except:
        callbackId = ""

    with app.app_context():
        game = app.cadaverGames[room]
        response.update({'origin':'payload'})
        response.update({'count': session['receive_count']})
        response.update({'data': game.toJSON()})
        response.update({'info':'Payload for room '+room})
        response.update({'callbackId': callbackId})

    emit('payload', response)



@socketio.event
def connect(message):

    global thread
    with thread_lock:
        if thread is None:
            thread = socketio.start_background_task(background_thread)

    tabID = request.args.get('tabID')
    #print("socketio",tabID)

    with app.app_context():
        for k, v in app.cadaverGames.items():
            if v.hasPlayer(tabID):
                print(f"Welcome back to your {k} room!",tabID)
                join_room(k)




@socketio.on('disconnect')
def test_disconnect():
    print('Client disconnected')


if __name__ == '__main__':
    socketio.run(app)