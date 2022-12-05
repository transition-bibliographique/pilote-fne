package fr.fne.batch.util;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ApiWB {
	private final Logger logger = LoggerFactory.getLogger(ApiWB.class);

	@Value("${wikibase.url}")
	private String urlWikiBase;
	@Value("${wikibase.bot.login}")
    private String lgname;
	@Value("${wikibase.bot.pwd}")
    private String lgpassword;
	//################## Version basique : sans librairie particuliere
    
    // Utilisation de RestTemplate get
    public JSONObject getJson(String url) throws Exception {
    	
		//Avec RestTemplate
		RestTemplate restTemplate = new RestTemplate();
		JSONObject json =  new JSONObject(restTemplate.getForObject(urlWikiBase + url, String.class));

		return json;


    }

	// Permet d'avoir une réponse Json à partir d'une requête Sparql
	public JSONObject getSparqlJson(String url, String queryParam) throws Exception {

		//Avec RestTemplate
		RestTemplate restTemplate = new RestTemplate();
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlWikiBase + url).queryParam("query",queryParam);
		JSONObject json =  new JSONObject(restTemplate.getForObject(builder.build().encode().toUri(), String.class));

		return json;
	}

    // Utilisation de RestTemplate post
    public JSONObject postJson(Map<String,String> params) throws Exception {
    	    	
		//Avec RestTemplate
    	RestTemplate restTemplate = new RestTemplate();
		HttpHeaders entete = new HttpHeaders();
		entete.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> paramE = new LinkedMultiValueMap<String, String>();
		for (Map.Entry<String,String> param : params.entrySet()) {
			paramE.add(param.getKey(),param.getValue());
		}

		HttpEntity<MultiValueMap<String, String>> requeteHttp = new HttpEntity<MultiValueMap<String, String>>(paramE, entete);		
		JSONObject json = new JSONObject(restTemplate.postForEntity(urlWikiBase, requeteHttp, String.class).getBody().toString());
        
        return json;
    }
    
  	//Connexion à WikiBase avec un compte Bot et cookie. Renvoie le csrftoken
    public String connexionWB() throws Exception {
		/* ## CONNECTION ## */
		// 1) Recuperer un login token
		// Le cookie est obligatoire sinon erreur
		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
		/*
		 * Pour info, lecture des valeurs du cookie : List<HttpCookie> cookies =
		 * cookieManager.getCookieStore().getCookies(); for (HttpCookie cookie :
		 * cookies) { logger.info(cookie.getDomain()); logger.info(cookie); }
		 */


		JSONObject json = this.getJson("?action=query&meta=tokens&type=login&format=json");
		//logger.info(json.toString());
		String loginToken = json.optJSONObject("query").optJSONObject("tokens").optString("logintoken");
		// logger.info(loginToken);

		// 2) se logger avec un POST data
		Map<String, String> params = new LinkedHashMap<>();
		params.put("action", "login");
		params.put("lgname", lgname);
		params.put("lgpassword", lgpassword);
		params.put("lgtoken", loginToken);
		params.put("format", "json");
		json = this.postJson(params);
		// logger.info(json.toString());

		//Récupèration du token CSRF : csrftoken si connexion OK
		json = this.getJson("?action=query&meta=tokens&format=json");
		String csrftoken = json.optJSONObject("query").optJSONObject("tokens").optString("csrftoken");

		return csrftoken;
	}
    
    
}