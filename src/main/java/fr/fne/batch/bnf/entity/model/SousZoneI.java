package fr.fne.batch.bnf.entity.model;

import java.util.Objects;

/**
 * Classe représentant une sous zone
 * On associe à chaque {@link SousZoneI} un code et une valeur
 *
 * @author Thibaud SENALADA
 * @author Mickael KWINE KWOR MAN
 *
 */
public class SousZoneI {

	private String code;
	private String valeur;

	/**
	 * Constructeur
	 */
	public SousZoneI() {
		// Constructeur vide
	}

	/**
	 * Constructeur
	 * 
	 * @param code
	 * 			{@link String} Code complet (ex: 245$a)
	 * @param valeur
	 */
	public SousZoneI(String code, String valeur) {
		this.code = code;
		this.valeur = valeur;
	}

	public String getValeur() {
		return this.valeur;
	}

	public SousZoneI setValeur(String valeur) {
		this.valeur = valeur;
		return this;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return this.code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return "SousZoneI [code=" + code + ", valeur=" + valeur + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, valeur);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SousZoneI)) {
			return false;
		}
		SousZoneI other = (SousZoneI) obj;
		return Objects.equals(code, other.code) && Objects.equals(valeur, other.valeur);
	}
}