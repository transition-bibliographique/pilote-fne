package fr.fne.batch.bnf.notice.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Représentation abstraite d'une <b>sous zone d'une zone</b> {@link ZoneN} d'une notice {@link Notice}<br>
 * 
 * <p><b>Attention</b> le nom de la classe peut être trompeur. 
 * Ce modele est conçu pour stocker des sous zones genre $a. En ce qui concerne les zones à position,
 * il faudra aller voir la classe {@link SousZoneN}
 * </p>
 * 
 * @author Thibaud SENALADA
 * 
 * @author Mickael KWINE KWOR MAN
 *
 */
public class SousElementN {

	private String code;

	private String value;

	/**
	 * Constructeur
	 * @param code
	 */
	public SousElementN(String code) {
		super();
		this.code = code;
	}	
	
	/*
	 *      __  __ ______ _______ _    _  ____  _____   _____ 
	 *     |  \/  |  ____|__   __| |  | |/ __ \|  __ \ / ____|
	 *     | \  / | |__     | |  | |__| | |  | | |  | | (___  
	 *     | |\/| |  __|    | |  |  __  | |  | | |  | |\___ \ 
	 *     | |  | | |____   | |  | |  | | |__| | |__| |____) |
	 *     |_|  |_|______|  |_|  |_|  |_|\____/|_____/|_____/ 
	 *                                                        
	 *                                                        
	 */

	/**
	 * Permet de savoir si la valeur du sous element est contenu dans la liste passé en paramètre.
	 * 
	 * @param values
	 * 		Array de {@link String} à tester contre la valeur du sous element.
	 * @return
	 * 		<code>true</code> si un des element de la liste est identique à la valeur du sous element sinon retourne <code>false</code>
	 */
	public boolean isEqualsTo(String... values) {
		List<String> listValue = Arrays.asList(values);
		for (String val : listValue) {
			if(val.equals(this.value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Permet de savoir si la valeur du sous element commence par l'une des valeurs passé en paramètre.
	 * 
	 * @param values
	 * 		Array de {@link String} à tester contre la valeur du sous element.
	 * @return
	 * 		<code>true</code> si un des element de la liste correspond au début de la valeur du sous element sinon retourne <code>false</code>
	 */
	public boolean startWith(String... values) {
		List<String> listValue = Arrays.asList(values);
		for (String val : listValue) {
			if(this.value != null && this.value.startsWith(val)) {
				return true;
			}
		}
		return false;
	}
	

	/**
	 * Indique si la valeur est égale au caractère "#" ou si la valeur est vide.
	 * 
	 * @return Boolean
	 */
	public Boolean hashOrEmpty() {
		return "#".equals(this.value) || "".equals(this.value);
	}
	
	/*
	 *       _____ ______ _______ _______ ______ _____   _____    _____ ______ _______ _______ ______ _____   _____ 
	 *      / ____|  ____|__   __|__   __|  ____|  __ \ / ____|  / ____|  ____|__   __|__   __|  ____|  __ \ / ____|
	 *     | |  __| |__     | |     | |  | |__  | |__) | (___   | (___ | |__     | |     | |  | |__  | |__) | (___  
	 *     | | |_ |  __|    | |     | |  |  __| |  _  / \___ \   \___ \|  __|    | |     | |  |  __| |  _  / \___ \ 
	 *     | |__| | |____   | |     | |  | |____| | \ \ ____) |  ____) | |____   | |     | |  | |____| | \ \ ____) |
	 *      \_____|______|  |_|     |_|  |______|_|  \_\_____/  |_____/|______|  |_|     |_|  |______|_|  \_\_____/ 
	 *                                                                                                              
	 *                                                                                                              
	 */

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	/*
	 *      ______ ____  _    _         _       _____ 
	 *     |  ____/ __ \| |  | |  /\   | |     / ____|
	 *     | |__ | |  | | |  | | /  \  | |    | (___  
	 *     |  __|| |  | | |  | |/ /\ \ | |     \___ \ 
	 *     | |___| |__| | |__| / ____ \| |____ ____) |
	 *     |______\___\_\\____/_/    \_\______|_____/ 
	 *                                                
	 *                                                
	 */

	@Override
	public int hashCode() {
		return Objects.hash(this.code, this.value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SousElementN)) {
			return false;
		}
		SousElementN other = (SousElementN) obj;
		return Objects.equals(this.code, other.code) && Objects.equals(this.value, other.value);
	}

	@Override
	public String toString() {
		return "SousElementN [code=" + code + ", value=" + value + "]";
	}

	
}
