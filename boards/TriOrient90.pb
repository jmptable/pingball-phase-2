#Tests a triangle bumper oriented at 90 degrees
board name=Half1 gravity=5.0

ball name=ball1 x=5.0 y = 6.0 xVelocity = 0.0 yVelocity = 0.0

triangleBumper name=tri x=19 y=2 orientation=90

absorber name=abs x=0 y=18 width=20 height=1

fire trigger=abs action=abs