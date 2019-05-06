package fr.utbm.gl52.droneSimulator.view;

import javafx.scene.shape.Shape;

interface GraphicElementInterface {
    void setCoord(Float[] _coord);
    void setWidth(Float _width);
    void setHeight(Float _height);
    void isFilled(boolean b);
    void setColor(String s);

    Float[] getCoord();
    Boolean isFilled();
    Float getHeight();
    Float getWidth();
    Float getX();
    Float getY();
    Shape getShape();
    String getColor();
}