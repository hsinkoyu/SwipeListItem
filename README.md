SwipeListItem - Make an Android list view item swipable

The concept of swiping a list item is straightforward. There are two types
of background views displaying.


                     Type 1: Pull Out

         Offset both the foreground (mCenter) and 
         backgrounds (mTop | mBottom | mRight | mLeft)
         views' location.

                     +----------------+
                     |                |
                     |      mTop      |
                     |                |
    +----------------+----------------+----------------+
    |                |<-Screen width->|                |
    |     mLeft      |     mCenter    |     mRight     |
    |                |Ex. A list item |                |
    +----------------+----------------+----------------+
                     |                |
                     |     mBottom    |
                     |                |
                     +----------------+


                     Type 2: Open Page

           Set the visibility of backgrounds, and
           offset the foreground view's location.

    +------------------------------------------------+
    |<--------------- Screen width ----------------->|
    |                                                |
    |                                                |
    |                                                |
    |             Foreground:  mCenter               |
    |                                                |
    |             Backgrounds: mTop                  |
    |                          mBottom               |
    |                          mRight                |
    |                          mLeft                 |
    |                                                |
    |                                                |
    |                                                |
    +------------------------------------------------+


So the core of code is offsetLeftAndRight().


The interface OnSwipeListener is the callback of moving views.
You can add the code to take appropriate actions when moving views.

Have fun.
Hsinko Yu <hsinkoyu@gmail.com>

