import sys

class ApolloMessage(object):
    def __init__(self, msg):
        for k, v in msg.items():
            print('setting {}: {}'.format(k, v))
            sys.stdout.flush()
            setattr(self, k, v)
