import json
from random import randint, choice
from threading import Lock
from flask import Flask, render_template, session, request, \
    copy_current_request_context
from flask_socketio import SocketIO, emit, join_room, leave_room, \
    close_room, rooms, disconnect


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
        self.status = "waiting"

    def joinGame(self, playerId, name, avatar, isAdmin):
        p = {"playerId": playerId, "name": name, "avatar": avatar, "isAdmin": isAdmin}
        self.players.append(p)

    def hasPlayer(self, playerId):
        pExists = False
        for p in self.players:
            if p['playerId'] == playerId:
                pExists = True  
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

    def giveAdmin(self, playerId):
        for p in self.players:
            if p['playerId'] == playerId:
                pToAdmin = p
        if p:
            p['isAdmin'] = True

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
socketio = SocketIO(app, async_mode=async_mode)
thread = None
thread_lock = Lock()


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
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': message['data'], 'count': session['receive_count']})


@socketio.event
def my_broadcast_event(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': message['data'], 'count': session['receive_count']},
         broadcast=True)


@socketio.event
def joinGame(message):

    join_room(message['room'])
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': 'In rooms: ' + ', '.join(rooms()),
        'count': session['receive_count']})

    with app.app_context():
        try:
            if not app.game.hasPlayer(request.sid):
                app.game.joinGame(request.sid, request.sid, "avatar", False)
                if len(app.game.players) == 1:
                    app.game.giveAdmin[request.sid] 
        except:
            app.game = CadaverGame(randint(0,1000),message['room'],2048,'https://')
            app.game.joinGame(request.sid, request.sid, "avatar", True)

        print(app.game.toJSON())



@socketio.event
def leave(message):
    leave_room(message['room'])
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': 'In rooms: ' + ', '.join(rooms()),
          'count': session['receive_count']})

    with app.app_context():
        try:
            print("playerId",request.sid)
            app.game.leaveGame(request.sid)
        except:
            pass

#        print(app.game.toJSON())


@socketio.on('close_room')
def on_close_room(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response', {'data': 'Room ' + message['room'] + ' is closing.',
                         'count': session['receive_count']},
         to=message['room'])
    close_room(message['room'])


@socketio.event
def my_room_event(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': message['data'], 'count': session['receive_count']},
         to=message['room'])


@socketio.event
def disconnect_request():
    @copy_current_request_context
    def can_disconnect():
        disconnect()

    session['receive_count'] = session.get('receive_count', 0) + 1
    # for this emit we use a callback function
    # when the callback function is invoked we know that the message has been
    # received and it is safe to disconnect
    emit('my_response',
         {'data': 'Disconnected!', 'count': session['receive_count']},
         callback=can_disconnect)

@socketio.event
def payload_request():

    with app.app_context():
        try:
            payload = app.game.toJSON()
        except:
            pass
    emit('my_response',
         {'data': payload, 'count': session['receive_count']})


@socketio.event
def my_ping():
    emit('my_pong')


@socketio.event
def connect():
    global thread
    with thread_lock:
        if thread is None:
            thread = socketio.start_background_task(background_thread)
    emit('my_response', {'data': 'Connected', 'count': 0})


@socketio.on('disconnect')
def test_disconnect():
    print('Client disconnected', request.sid)


if __name__ == '__main__':
    socketio.run(app)