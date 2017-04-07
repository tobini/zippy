class Foo(object):
    def __init__(self):
        self.bar = 2

    def change_bar(self):
        test = 5
        self.bar = test
        return True


print(Foo().change_bar())