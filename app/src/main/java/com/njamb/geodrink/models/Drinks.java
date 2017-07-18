package com.njamb.geodrink.models;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by njamb94 on 7/16/2017.
 */

@IgnoreExtraProperties
public class Drinks {
    public long beer;
    public long coffee;
    public long cocktail;
    public long juice;
    public long soda;
    public long alcohol;

    public Drinks() {
//        beer = 0;
//        coffee = 0;
//        cocktail = 0;
//        juice = 0;
//        soda = 0;
//        alcohol = 0;
    }

    public Drinks(long beer, long coffee, long cocktail, long juice, long soda, long alcohol) {
        this.beer = beer;
        this.coffee = coffee;
        this.cocktail = cocktail;
        this.juice = juice;
        this.soda = soda;
        this.alcohol = alcohol;
    }

    @Override
    public String toString() {
        String str;

        str = "\n\nBeer: " + beer + "\nCoffee: " + coffee + "\nCocktail: " + cocktail +
                "\nJuice: " + juice + "\nSoda: " + soda + "\nAlcohol: " + alcohol + "\n\n";
        return str;
    }

    public void incrementDrinkByName(String drink) {
        switch (drink.toLowerCase()) {
            case "beer": { beer++; break; }
            case "coffee" : { coffee++; break; }
            case "cocktail" : { cocktail++; break; }
            case "juice" : { juice++; break; }
            case "soda" : { soda++; break; }
            case "alcohol" : { alcohol++; break; }
            default: break;
        }
    }
}
