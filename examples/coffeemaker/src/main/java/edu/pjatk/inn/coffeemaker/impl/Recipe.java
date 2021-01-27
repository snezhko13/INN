package edu.pjatk.inn.coffeemaker.impl;

import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Class that desribes single recipe with name, price and ingredients necesary.
 * @author   Sarah & Mike
 */
public class Recipe implements Serializable {
    private String name;
    private int price;
    private int amtCoffee;
    private int amtMilk;
    private int amtSugar;
    private int amtChocolate;

	/**
	 * Constructor.
	 * Creates a new Recipe with default values
	 */
	public Recipe() {
    	this.name = "";
    	this.price = 0;
    	this.amtCoffee = 0;
    	this.amtMilk = 0;
    	this.amtSugar = 0;
    	this.amtChocolate = 0;
    }
    
    /**
	 * Amount of chocolate.
	 *
	 * @return   Returns the amtChocolate.
	 */
    public int getAmtChocolate() {
		return amtChocolate;
	}

    /**
	 * Sets new amount of chocolate. Does nothing when param 'amtChocolate' is less than 0.
	 *
	 * @param amtChocolate   The amtChocolate to setValue.
	 */
    public void setAmtChocolate(int amtChocolate) {
		if (amtChocolate >= 0) {
			this.amtChocolate = amtChocolate;
		} 
	}

    /**
	 * Amount of coffee.
	 *
	 * @return   Returns the amtCoffee.
	 */
    public int getAmtCoffee() {
		return amtCoffee;
	}

    /**
	 * Sets new amount of coffee. Does nothing when param 'amtCoffee' is less than 0.
	 *
	 * @param amtCoffee   The amtCoffee to setValue.
	 */
    public void setAmtCoffee(int amtCoffee) {
		if (amtCoffee >= 0) {
			this.amtCoffee = amtCoffee;
		} 
	}

    /**
	 * Amount of milk.
	 *
	 * @return   Returns the amtMilk.
	 */
    public int getAmtMilk() {
		return amtMilk;
	}

    /**
	 * Sets new amount of milk. Does nothing when param 'amtMilk' is less than 0.
	 *
	 * @param amtMilk   The amtMilk to setValue.
	 */
    public void setAmtMilk(int amtMilk) {
		if (amtMilk >= 0) {
			this.amtMilk = amtMilk;
		} 
	}

    /**
	 * Amount of sugar.
	 *
	 * @return	Returns the amtSugar.
	 */
    public int getAmtSugar() {
		return amtSugar;
	}

    /**
	 * Sets new amount of sugar. Does nothing when 'amtSugar' is less than 0.
	 *
	 * @param amtSugar   The amtSugar to setValue.
	 */
    public void setAmtSugar(int amtSugar) {
		if (amtSugar >= 0) {
			this.amtSugar = amtSugar;
		} 
	}

    /**
	 * Name of the recipe.
	 *
	 * @return	Returns the key.
	 */
    public String getName() {
		return name;
	}

    /**
	 * Sets new name of the Recipe. Does nothing when param 'name' is null.
	 *
	 * @param name   The key to setValue.
	 */
    public void setName(String name) {
    	if(name != null) {
    		this.name = name;
    	}
	}

    /**
	 * Price of drink based on this recipe.
	 *
	 * @return   Returns the price.
	 */
    public int getPrice() {
		return price;
	}

    /**
	 * Sets new price of drink based on this recipe. Does nothing when param 'price' is less than 0.
	 *
	 * @param price   The price to setValue.
	 */
    public void setPrice(int price) {
		if (price >= 0) {
			this.price = price;
		} 
	}

	/**
	 * Compares names of this instance and input Recipe instance. Returns true if names are equal.
	 *
	 * @param r	instance of Recipe class.
	 * @return boolean
	 */
    public boolean equals(Recipe r) {
        if((this.name).equals(r.getName())) {
            return true;
        }
        return false;
    }

	/**
	 * Returns name of Recipe.
	 *
	 * @return String
	 */
	public String toString() {
    	return name;
    }

	/**
	 * Creates Recipe instance from given context.
	 *
	 * @param context	Provided context.
	 * @return r		Recipe obtained from context.
	 * @throws ContextException	Exception thrown when Recipe could not be obtained.
	 */
	static public Recipe getRecipe(Context context) throws ContextException {
		Recipe r = new Recipe();
		try {
			r.name = (String)context.getValue("key");
			r.price = (int)context.getValue("price");
			r.amtCoffee = (int)context.getValue("amtCoffee");
			r.amtMilk = (int)context.getValue("amtMilk");
			r.amtSugar = (int)context.getValue("amtSugar");
			r.amtChocolate = (int)context.getValue("amtChocolate");
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		return r;
	}

	/**
	 * Returns the context with given recipe
	 *
	 * @param recipe	the context identification
	 * @return ctx		the context for the method
	 * @throws ContextException
	 */
	static public Context getContext(Recipe recipe) throws ContextException {
		Context cxt = new ServiceContext();
		cxt.putValue("key", recipe.getName());
		cxt.putValue("price", recipe.getPrice());
		cxt.putValue("amtCoffee", recipe.getAmtCoffee());
		cxt.putValue("amtMilk", recipe.getAmtMilk());
		cxt.putValue("amtSugar", recipe.getAmtSugar());
		cxt.putValue("amtChocolate", recipe.getAmtChocolate());
		return cxt;
	}

}
