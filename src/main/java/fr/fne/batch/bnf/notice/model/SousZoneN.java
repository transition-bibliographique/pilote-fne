package fr.fne.batch.bnf.notice.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Représentation abstraite d'une <b>sous zone à position</b>.
 * Voir {@link SousElementN}
 * 
 * @author Thibaud SENALADA
 * 
 * @author Mickael KWINE KWOR MAN
 *
 */
public class SousZoneN extends SousElementN {
	
	private String numero;

	private static final Logger logger = LoggerFactory.getLogger(SousZoneN.class);
	
	private String barre;
	
	private List<PosN> posNs;
	
	private String sens;
	
	/**
	 * Constructeur.
	 * Voir un exemple de noticeservice pour comprendre la notion de barre et de sens.
	 *  
	 * @param code
	 * 			{@link String} Le code de la sous zone
	 * @param barre
	 * 			{@link String} Barre de la sous zone
	 * @param sens
	 * 			{@link String} Sens de la sous zone
	 */
	public SousZoneN(String code, String barre, String sens) {
		super(code);
		this.setBarre(barre);
		this.sens = sens;
		this.posNs = new ArrayList<>();
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
	
	public void setNumero(String numero) {
		this.numero = numero;
	}
	
	public String getBarre() {
		return barre;
	}

	public void setBarre(String barre) {
		this.barre = barre;
	}

	public List<PosN> getPosNs() {
		return posNs;
	}

	public void setPosNs(List<PosN> posNs) {
		this.posNs = posNs;
	}
	
	public String getSens() {
		return this.sens;
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
	
	@Override
	public String getValue() {
		// Dans le cas ou il n'y a pas de barre à positionner
		if(this.barre == null) {
			return super.getValue();
		}else {
			try {
				int position = Integer.parseInt(this.barre);
				StringBuilder sb = new StringBuilder(super.getValue());
				sb.insert(position, '|'); // On rajoute une barre à la position renseigné en attribut
				return sb.toString();
			} catch (NumberFormatException | IndexOutOfBoundsException  e) {
				// Dans le cas ou la position n'est pas bien renseigné ou bien qu'elle est plus grande que la taille du texte
				if (logger.isInfoEnabled()) {
					logger.info(String.format("[%s] La valeur de barre %s n'est pas possible : %s", this.numero, this.barre, e.getMessage()));
				}				
				return super.getValue();
			}	
		}
	}
	
	public boolean isValueInPosN(String codePosN, String value) {
		Optional<PosN> posOpt = posNs.stream().filter(p -> p.getCode().equals(codePosN)).findFirst();
		return posOpt.isPresent() && posOpt.get().getValue().equals(value);
	}
	
	public boolean isOneValueInPosN(String codePosN, String... values) {
		Optional<PosN> posOpt = posNs.stream().filter(p -> p.getCode().equals(codePosN)).findFirst();
		return posOpt.isPresent() && Stream.of(values).anyMatch(v -> posOpt.get().getValue().equals(v));
	}
	
	public Optional<String> getPosNValueByCode(String code) {
		Optional<PosN> posOpt = posNs.stream().filter(p -> p.getCode().equals(code)).findFirst();
		if (posOpt.isPresent()) {
			return Optional.of(posOpt.get().getValue());
		} else {
			return Optional.empty();
		}
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
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(barre, posNs, sens);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof SousZoneN)) {
			return false;
		}
		SousZoneN other = (SousZoneN) obj;
		return Objects.equals(barre, other.barre) && Objects.equals(posNs, other.posNs) && Objects.equals(sens, other.sens);
	}
}
