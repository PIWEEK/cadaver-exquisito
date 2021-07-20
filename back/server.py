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

    return drawings    

def generateAvatars(multiples=5):
    shape = [1,2,3,4,5,6,7,8,9]
    color = ["a","b","c","d","e","f","g","h","i","j","k"]


    if multiples < 1:
        multiples = 1
    shapes = []
    colors = []
    for i in range(multiples):
        shuffle(shape)
        shapes+=shape[:]
        shuffle(color)
        colors+=color[:]

    avatars = list(zip(shapes, colors))
    print(avatars)
    return avatars




class CadaverGame:

    def __init__(self, gameId, name, canvasWidth, URL):

        self.gameId = gameId
        self.name = name
        self.canvasWidth = canvasWidth
        self.URL = URL

        self.players = []
        self.canvasTurns =[]
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
        
    def buildAvatars(self):
        self.avatars = generateAvatars()

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
def my_event(message):
    
    response = {}
    session['receive_count'] = session.get('receive_count', 0) + 1

    response.update({'count': session['receive_count']})
    response.update({'type': 'my_event'})
    response.update({'data': message['data']})

    emit('my_response', response)


@socketio.event
def my_broadcast_event(message):

    response = {}
    session['receive_count'] = session.get('receive_count', 0) + 1

    response.update({'count': session['receive_count']})
    response.update({'type': 'my_broadcast_event'})
    response.update({'data': message['data']})
    
    emit('my_response', response, broadcast=True)


@socketio.event
def joinGame(message):

    response = {}
    room = message['room']

    join_room(room) # socket room

    session['receive_count'] = session.get('receive_count', 0) + 1

    response.update({'count': session['receive_count']})
    response.update({'type': 'joinGame'})
    response.update({'data': f'{request.sid} joined game: ' + room})
    
    with app.app_context():

        if room not in app.cadaverGames.keys():
            game = CadaverGame(randint(0,1000),room,2048,'https://')
            game.joinGame(request.sid, request.sid, True)
            app.cadaverGames[room] = game

        if not app.cadaverGames[room].hasPlayer(request.sid):
            app.cadaverGames[room].joinGame(request.sid, request.sid, False)
            if len(app.cadaverGames[room].players) == 1:
                app.cadaverGames[room].giveAdmin[request.sid] 

        print(app.cadaverGames[room].toJSON())

    emit('my_response', response ,to=room)


@socketio.event
def startGame(message):

    room = message['room']
    response = {}

    response.update({'count': session['receive_count']})
    response.update({'type':'startGame'})
    response.update({'gameId': room})

    with app.app_context():

        #TODO check for existing room, raise error otherwise

        game = app.cadaverGames[room]
        game.startGame()
    
    print(app.cadaverGames[room].toJSON())

    emit('response_startGame', response, to=room)



@socketio.event
def leaveGame(message):

    response = {}
    room = message['room']
    try:
        callbackId = message['callbackId']
    except:
        callbackId = ""

    session['receive_count'] = session.get('receive_count', 0) + 1
    response.update({'count': session['receive_count']})
    response.update({'type':'leaveGame'})
    response.update({'data':f'{request.sid} left the room '+room})
    response.update({'gameId': room})
    
    # try:
    #     callbackId = message['callbackId']
    # except:
    #     callbackId = ""

    # response.update({'callbackId': callbackId})
  
    emit('my_response', response, to=room)

    leave_room(message['room']) # socket room

    with app.app_context():
        app.cadaverGames[room].leaveGame(request.sid)
       

@socketio.on('close_room')
def on_close_room(message):

    response = {}
    session['receive_count'] = session.get('receive_count', 0) + 1
    response.update({'count': session['receive_count']})
    response.update({'data': 'Room ' + message['room'] + ' is closing.'})
    response.update({'type':'close_room'})

    close_room(message['room'])

    emit('my_response', response, to=message['room'])

    

@socketio.event
def my_room_event(message):

    response = {}
    session['receive_count'] = session.get('receive_count', 0) + 1
    response.update({'count': session['receive_count']})
    response.update({'data': message['data']})
    response.update({'type': 'my_room_event'})
    
    emit('my_response', response, to=message['room'])


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

@socketio.event
def payload_request(message):

    room = message['room']
    response = {}
    try:
        callbackId = message['callbackId']
    except:
        callbackId = ""

    with app.app_context():

        response.update({'type':'payload'})
        response.update({'gameId': room})
        response.update({'count': session['receive_count']})
        response.update({'data': app.cadaverGames[room].toJSON()})
        response.update({'callbackId': callbackId})

    emit('payload', response)


@socketio.event
def my_ping():
    emit('my_pong', {'type':'my_ping'})


@socketio.event
def connect(message):
    global thread
    with thread_lock:
        if thread is None:
            thread = socketio.start_background_task(background_thread)

    response = {}

    response.update({'count':0})
    response.update({'data': 'Connected'})
    response.update({'type': 'connect'})

    print(message)
    emit('my_response', response)


@socketio.on('disconnect')
def test_disconnect():
    print('Client disconnected', request.sid)


if __name__ == '__main__':
    socketio.run(app)