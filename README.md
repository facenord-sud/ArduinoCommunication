# ArduinoCommunication
> Bibliothèque facilitant l'interaction avec un Arduino pour une application Web

## Description
Cette bibliothèque comprend toutes les fonctionnalités utiles à l’interaction avec l’Arduino. Cela va de la connexion à un port série à la transformations d’objets Java en informations compréhensibles pour l’Arduino, en passant par une suite de tests adaptée à l’interaction avec l’Arduino. De plus, nous avons également inclus des classes simplifiant la gestion des notifications.

Dans cette bibliothèque, nous partons du principe que l’Arduino est relié à l'application Web par un port série via un câble USB. Puisqu'il nous fallait pouvoir écrire et lire des informations sur le port série, nous avons utilisé la bibliothèque [RxTx](http://rxtx.org). Mais cette bibliothèque est trop généraliste. Il a donc été nécessaire de créer notre propre ensemble de classes adaptées à notre cas spécifique. Cela signifie:

* Se connecter facilement au port série
* Pouvoir y lire et écrire
* Implémenter notre propre pattern observateur pour savoir quand de nouvelles informations sont disponibles
* Encoder et décoder des données en JSON
* facilement interagir avec les informations des différents composants de l’Arduino.

Au final, l’interaction avec l’Arduino doit être tout aussi facile qu’avec une base de données traditionnelle.

La section "Simulation" explique comment il est possible de simuler un Arduino. Un des premiers usages de la simulation est de pouvoir tester les applications développées sans dépendre de l’Arduino. Afin d’écrire facilement des tests utilisant la simulation de l’Arduino, nous avons défini un ensemble de fonctionnalités.

Finalement, nous avons défini quelques fonctionnalités pour gérer les notification. C’est à dire, fournir un moyen d’enregistrer les clients auxquels envoyer des notifications, envoyer les notifications aux clients et utiliser le design pattern builder pour déterminer quand et comment envoyer une notification en fonction des événements.

Le diagramme ci-dessous synthétise de quelle manière ces fonctionnalités sont structurées dans la bibliothèque ArduinoCommunication

![](https://raw.githubusercontent.com/facenord-sud/ArduinoCommunication/master/arduino_com.jpg)

## Motivation
La première motivation est de créer un ensemble de fonctionnalités, spécifiques à notre cas, en utilisant celles de RxTX, pour interagir avec l’Arduino connecté au port série. Ainsi, nous permettons la réutilisation de code, le code de l’application est beaucoup plus lisible et nous pouvons réduire le nombre d’erreurs potentielles.

La deuxième motivation est de faciliter la création de tests adaptés au cas d’un Arduino connecté à l’application. Les motivations à l’écriture de tests sont une évidence. Grâce à notre système, le code nécessaire pour écrire des tests est moins importants tout en évitant des erreurs. Ainsi, notre système encourage et simplifie l’écriture de tests.

Finalement, la dernière motivation est de simplifier la gestion des notifications en stockant les clients enregistrés, en envoyant les notifications et en implémentant le pattern observateur. Ainsi, pour l’implémentation de futures applications du Web des Objets, il ne sera plus nécessaire de réfléchir comment implémenter les notifications, mais uniquement remplir les vides d’une classe type. A nouveau notre système encourage la réutilisation du code. Si, par la suite un système pour stocker les clients dans une base de données est développé, ou que l’envoi des notifications est amélioré grâce au multi-threading, toutes les applications utilisant cette bibliothèque en profiteront.

Il y a encore l’avantage relatif a toute bibliothèque qui est que le code est utilisé et testé en situation réelle, réduisant ainsi le risque d’erreurs (par opposition à la même fonctionnalité implémentée par chacun dans son coin).

## Protocole de communication
Avant tout chose, nous avons défini un format de donnée pour structurer les informations échangées entre l’Arduino et le serveur.

Nous avons voulu définir un protocole de communication qui soit d’une part utilisable pour n’importe quel cas d’implémentation et qui soit d’autre part facile à implémenter. De plus, il y a également une volonté de limiter la quantité de données échangées.

### Description du protocole
Pour structurer les données échangées, nous nous basons sur une propriété du JSON et une de l’Arduino. Premièrement, en JSON, un certain type de donnée (Chaîne de caractères, nombre entier ou réel, sous-élément JSON, etc) est identifié par une clé pouvant être composée de chiffres et de lettres. Deuxièmement l’Arduino possède un certain nombre de connecteurs tous identifiés par un nombre. Dans le langage Arduino, ces nombres sont représentés par des constantes. Nous avons défini ainsi le principe suivant : Toutes les informations relatives à un composant sont regroupées dans un sous-élément JSON qui est identifié par le numéro du pin auquel est connecté le composant produisant ces informations.

Par exemple, pour le composant qui est le moteur du rideau de fer, produisant comme informations la vitesse à laquelle il tourne, la sous-chaîne JSON est la suivante :
```json
{"speed": 90}
```
Comme le moteur est connecté au pin O1, identifié par le nombre dix, la sous-chaîne est identifiée par le nombre dix et ajoutée à la chaîne principale échangée avec le serveur. Au final, nous obtenons du JSON structuré de la sorte :
```json
{"10": {"speed": 90}, "14": {"value": 0, "oldValue": 500}, ...}
```
Où `...` signifie que d’autres sous-éléments identifiés par le nombre d’un pin peuvent exister. Le deuxième élément, identifié par le nombre 14, représente un autre composant connecté au connecteur I1 (connecteur d’entrée de l’Arduino, sur lequel peut être branché différents type de senseur.

L’avantage d’un tel système est que d’une part, il est possible de représenter tous les composants d’un Arduino quelque soit le cas d’implémentation. Puisque chaque composant est forcément relié d’une manière unique à l’Arduino et donc identifiable de manière unique. D’autre part, ce principe permet d’imbriquer à l’infini des informations dans des informations sans limite de complexité. Cependant, les contraintes matérielles de l’Arduino limitent rapidement le nombre de données échangées.

Du côté de l’Arduino, l’implémentation est extrêmement simple. Coté Java, grâce aux bibliothèques ArduinoComunication et [ArduinoComponents](https://github.com/facenord-sud/ArduinoComponents), il est possible de travailler uniquement avec des objets Java. Les valeurs des variables d’instance de ces objets sont en fonction du JSON à décoder ou à encoder. Cependant, pour encoder et décoder le bon objet en fonction d’un composant, il est nécessaire de connaitre le pin sur lequel est branché ce composant. Pour ce faire, nous avons défini un enum, `TinkerShield`, dont le nom de ses variables correspond au noms des pins (O1..O5 et I1 .. I5) et la valeur de ses variables à la valeur du pin sur l’Arduino. Par exemple, `TinkerShield.o_1` retourne le nombre entier 10.


## Interaction avec l'Arduino
Décrit comment l'interaction avec l'Arduino a été implémentée et comment utiliser les fonctionalités développées.

### Implémentation
Les fonctionnalités pour interagir avec l’Arduino sont réparties dans trois classes, suivant leur degré d’abstraction. Le diagramme ci-dessous montre les relations entre ces classes et leurs méthodes.

![](https://raw.githubusercontent.com/facenord-sud/ArduinoCommunication/master/test.jpg)

#### Première couche d'abstraction
La première classe, `RxTxConnection`, permet la connexion/déconnexion au port série, la lecture et l’écriture d’informations du port série et implémente un observateur pour la réception d’informations envoyées par l’Arduino. RxTx propose déjà un observateur, mais uniquement un observant à la fois peut l’utiliser. Notre observateur se base donc sur celui de RxTx, mais permet un nombre illimité d’observant. Un dernier point est que cette classe est un singleton autorisant une unique connexion au port série dans toute l’application.

Il est important de noter la manière dont les informations sont lues du port série. Dès qu’une nouvelle information est disponible sur le port série, l’observeur de RxTx lance un événement et nous enregistrons l’information reçues dans une variable d’instance de la classe RxTX. Ce faisant, cela nous permet de ne jamais manquer une information, de toujours disposer d’une information quelconque, à part au lancement de l’application, et d’être simple. Une mauvais solution aurait été d’aller lire les informations sur le port série à chaque fois qu’une autre classe de l’application a besoin d’informations de l’Arduino. Cette (mauvaise) solution demanderait un code plus compliqué pour obtenir le même résultat. En somme, cette classe est utilisé pour améliorer et simplifier l’usage de RxTx dans notre cas spécifique.

#### Deuxième couche d'abstraction
La deuxième classe, `ArduinoCommunication`, utilise la première et permet, grâce à la bibliothèque [Gson](https://sites.google.com/site/gson/gson-user-guide), d’encoder/décoder en JSON les informations relatives au port série. Cette classe a pour unique but de simplifier l’usage de la bibliothèque Gson pour notre cas.

#### Toisième couche d'abstraction
La troisième et dernière classe, `RxtxUtils`, permet d’encoder/décoder en JSON des objets Java en se basant sur la classe `ArduinoCommunication`.

L’encodage et le décodage d’objets Java en JSON est géré grâce à la bibliothèque Gson de Google. Les methodes `addComponent` et `getComponent`, de `RxtxUtils`, utilisent les types génériques et permettent ainsi une transformation aisée entre des composants Arduino, encodés en JSON, en objets Java représentant ces composants. Un aspect qui mérite d’être relevé est que pour permettre d’envoyer en même temps les information de plusieurs composants à l’Arduino, il est nécessaire de l’effectuer en deux étapes:
1. La méthode addComponent permet de stocker dans une variables les différents objets à envoyer. Techniquement, ces objets Java sont transformés en instance de classe JsonElement et ajoutés à une instance de la classe JsonComponent, qui appartiennent toutes deux à la bibliothèque Gson,
2. A l’appel de la méthode `send`, les objets stockés sont transformés en JSON et envoyés à l’Arduino.

### Utilisation de l'interaction avec l'Arduino
L’interaction avec l’Arduino peut s’effectuer de plusieurs manière, suivant la classe utilisée, `RxtxConnection`, `ArduinoConnection` ou `RxtxUtils`. Dans cette section, nous détaillons uniquement l’usage de la classe `RxtxUtils`, c’est normalement la seule classe utilisée et la plus facile d’emploi.

#### Obtenir l'état d'un composant
Pour obtenir une instance d’une classe Java à partir d’un élément JSON représentant l’état d’un composant Arduino, deux étapes sont nécessairs:

1. Définir une classe possédant des attributs du même nom que les éléments du JSON qui nous intéresse. Les types des variables d’instance doivent être des types simples (int, float, boolean, String, ...). Cette classe a peut-être déjà été définie dans la dépendence Maven ArduinoComponents à la section5.1.3
2. Appeler la méthode getComponent de RxtxUtils.

La définition d’une classe est un processus standard et nous ne le commenterons pas ici. La méthode `getComponent` requiert deux arguments, le premier doit être le type de l’objet que l’on cherche à récupérer et le deuxième le pin, identifié par un nombre, sur lequel est connecté le composant Arduino. Pour le nombre du pin, l’enum `TinkerShield`, est d’une grande aide. Le code ci-dessous est un exemple standard de comment le réaliser et assume que la classe `LinearPotentiometer` fait partie de la dépendance [ArduinoComponents](https://github.com/facenord-sud/ArduinoComponents).
```java
￼RxtxUtils utils = new RxtxUtils();
LinearPotentiometer pot = utils.getComponent(LinearPotentiometer.class, TinkerShield.i_0);
```
Si pour une raison ou une autre, il n’est pas possible de retourner une instance de la classe `LinearPotentiometer`, JSON mal formé, pas d’information de l’Arduino, etc, la valeur est nulle. A noter encore que le JSON envoyé par l’Arduino est équivalent à :
```json
{"14": {"oldPosition": 0, "position": 1023}}
```

#### Modifier l'état d'un composant
Transmettre les valeurs des variables d’instance d’une classe Java n’est pas plus compliqué. En partant du principe qu’un objet Java contenant une ou plusieurs variables d’instance de type simple (int, float, boolean, String, ...) existe, il suffit d’appeler la méthode `addComponent`, puis si l’on désire encoder d’autres objets, appeler cette méthode à volonté. Afin de transmettre tous les objets encodés précédemment, il ne reste plus qu’à appeler la méthode `send`. Le code ci-dessous donne un exemple de la manière d’encoder deux objets et de les envoyer à l’Arduino.
```java
RxtxUtils utils = new RxtxUtils();
ContiniousServo cs1 = new ContiniousServo();
ContiniousServo cs2 = new ContiniousServo();
cs1.setSpeed(ContiniousServo.OPEN_MAX_SPEED);
cs2.setSpeed(ContiniousServo.CLOSE_MAX_SPEED);
utils.addComponent(TinkerShield.o_1, cs1);
utils.addComponent(TinkerShield.o_2, cs2);
utils.send();
```
Le JSON produit sera de la forme:
```json
{"10": {"speed": 180}, "11": {"speed": 0}}
```
Aux lignes numéro six et sept de l'exemple ci-dessus, nous observons que la méthode `addComponent` prends deux arguments. Le premier indique le numéro du port de l’Arduino auquel est branché l’Arduino et le deuxième est l’objet à encoder. La méthode `send` de la ligne numéro huit envoie les objets `cs1` et `cs2` à l’Arduino. C’est uniquement à ce moment-là que des données sont transmises à l’Arduino.

## Notifications
La gestion des notifications se décompose en deux parties. La première permet d’enregistrer les clients désireux de recevoir des notifications dans un HashMap. Cette partie est évidente est ne mérite pas de commentaires.
L’autre partie s’occupe de l’envoi des notifications.

Ainsi, l’utilisation du pattern builder permet une politique d’envoi des notifications différentes suivant les besoins de l’implémentation. Il suffit d’implémenter un builder différent pour chaque type de notification que l’on souhaite et l’assigner à une instance de la classe Notification. La méthode hasNotification, qui doit être redéfinie pour chaque builder, renvoie un booléen utilisé pour déterminer s’il faut notifier les clients.

Le principal défaut de notre gestion des notifications est la lenteur potentielle. Comme tout se passe dans un seul thread, les notifications sont envoyées une à une à chaque clients enregistrés. Nous pouvons partir du principe qu’un client unique peut s’enregistrer à plusieurs types de notifications et que la durée de la requête effectuée vers chaque client dépend de la rapidité du client à y répondre, nous pouvons très rapidement avoir un problème de performance et de notifications perdues.

## Simulation de l'Arduino
Au début, pouvoir simuler le fait qu’un Arduino soit connecté au serveur, nous semblait relever du domaine des rêves. Nous rêvions de ceci afin de s’affranchir de l’Arduino pour l’implémentation du service Web et de l’interface client du rideau de fer. Simuler un Arduino, nous permet également d’écrire tout une série de tests et une nouvelle façon de développer une application du Web des Objets.

### Implémentation de la simulation
Toute la magie de la simulation d’un Arduino vient du programme [socat](http://www.dest-unreach.org/socat/) qui permet de simuler des ports séries.

Le principe est simple, nous créons deux ports séries connectés, appelés master et slave. Toutes les informations envoyées sur le premier port seront automatiquement disponibles sur le deuxième port et inversement. Normalement, une seule connexion par port série est autorisée. Mais l’utilisation de deux ports série, dont les mêmes informations sont disponibles en même temps sur les deux ports permet en quelque sorte deux connexions sur un seul port. C’est exactement cette caractéristique qui nous intéresse, puisque nous avons besoin d’une connexion pour simuler un Arduino et d’une connexion utilisée par la bibliothèque ArduinoCommunication. Le diagramme ci-dessous met évidence ce principe.

![](https://raw.githubusercontent.com/facenord-sud/ArduinoCommunication/master/socat.jpg)

Nous connectons le port master à la classe HardwareSpeaker permettant de simuler l’envoi et la lecture d’informations par port série. HardwareSpeaker possède deux méthodes importantes, une pour envoyer des informations sur le port master, qui seront disponibles sur le port slave. L’autre méthode permet de lire les informations sur le port master, qui ont été envoyées par la bibliothèque ArduinCommunication sur le port slave et donc disponibles sur le port master. Par exemple, quand nous ordonnons à HardwareSpeaker d’envoyer des informations sur le port master, les informations sont disponibles sur le port slave, faisant penser que l’Arduino a envoyé ces informations.

La simulation d’un Arduino est réalisée dans trois classes, qui sont représentées à l’aide du diagramme ci-dessous.

![](https://raw.githubusercontent.com/facenord-sud/ArduinoCommunication/master/rxtx.jpg)

#### ConnectionSimulator
La première classe, ConnectionSimulator, permet d’effectuer plusieurs actions :

1. Lancer le programme socat
2. Vérifier que socat ait démarré correctement
3. Ajouter des propriétés système
4. Factory pour fournir une unique instance de la classe HardwareSpeaker. Cette classe est responsable de se connecter à un port, d’envoyer et de recevoir des données de ce port.
5. Arrêter le processus socat
￼
ConnectionSimulator est utilisé dans un contexte multi-thread, chaque méthode sensible utilise le mot-clé synchronized afin d’empêcher que deux threads accèdent en même temps à la même méthode. Socat est un programme disponible pour les systèmes d’exploitation Windows, Linux et Mac OS X. Cependant, son utilisation varie d’un OS à l’autre, c’est pourquoi dans ConnectionSimulator nous ne lançons pas socat de la même manière suivant que le code soit exécuté sous Mac OS X ou Linux. Pour Windows, le cas n’a pas été géré, puisque nous ne disposons pas de machine utilisant cet OS.

Un défaut de notre implémentation est que socat a besoin d’être installé séparément par l’utilisateur et disponible dans le chemin d’exécution. Afin de vérifier que socat ait bien démarré, nous lisons les informations envoyées par socat sur le flux d’erreurs standard. Si le texte envoyé ne correspond pas à certains critères, ou qu’après cinq secondes aucun texte n’a été envoyé, nous lançons l’erreur `SocatNotStartedError` afin d’avertir l’utilisateur que socat n’a pas pu être démarré correctement. Par défaut, RxTx, la librairie utilisé en Java pour interagir avec les ports séries, cherche à se connecter uniquement à un nombre restreint de ports. La propriété système `gnu.io.rxtx.SerialPorts` permet d’indiquer à RxTx de chercher à se connecter à d’autres ports. La deuxième propriété système que nous donnons est sert à indiquer à la classe responsable de la connexion à l’Arduino quel port utiliser. Il est encore utile de noter que la classe ConnectionSimulator peut s’utiliser comme Singleton ou non, suivant l’utilisation requise.

#### HardwareSpeaker
La deuxième classe créée pour simuler une connexion par port série, HardwareSpeaker, offre quatre fonctionnalités :

1. Se connecter au port série dont son nom est passé en paramètre dans l’instance de la classe
2. Envoyer des données au port série
3. Lire des données du port série
4. Se déconnecter du port série précédemment connecté.
￼

Pour cette classe, un point important est à relever. Durant le développement, nous avons remarqué que si deux messages sont envoyés dans un espace de temps très court au port série, le premier message ne sera jamais reçu et sera écrasé par le second. Pour pallier à ce problème, nous effectuons une boucle, durant au maximum cinq secondes, tant que le message n’a pas été envoyé au port série. Le code ci-dessous explique en pseudo-code comment cela est réalisé.
```
￼loop while the port is null and the message on the port is not equal to the given one
    increment maxIteration by one
    if maxIteration is bigger than 5000
       break the loop
    end if
wait 1ms
￼end loop
```

### AbstractJaxbFactory
Les deux classes HardwareSpeaker et ConnectionSimulator sont généralistes et peuvent être utilisées pour simuler n’importe quelle connexion par port série. Cependant, la troisième classe,AbstractJaxbFactory, permet de simuler un Arduino. Le principe est simple, à chaque appel de la méthode send, l’objet Java passé en deuxième paramètre est encodé en un élément JSON représentant un composant. Comme nous l’avons vu à la section ci-dessus, chaque élément JSON représentant un composant doit être identifié par la valeur du pin auquel il est connecté. Pour ce faire, nous utilisons l’enum TinkerShield qui dans ce cas là, retourne la valeur 9. Dans la méthode send, la chaîne de caractères ainsi obtenue est passée en paramètre de la méthode speak de HardwareSpeaker. Comme le montre le code ci-dessous une utilisation de cette classe peut être faite en simplifiant la création de Factory, qui étend de AbstractJaxbFactory, simulant différents états de l’Arduino.

```￼java
public class JaxbTestFactory extends AbstractJaxbFactory{
  public JaxbTestFactory(HardwareSpeaker hardware) {
     super(hardware);
   }
   // simulate that the door is full unlocked
   public void createLockOpen() {
     // It is a class from the ArduinoComponents library
     // which represent a linear potentiometer
     LinearPotentiometer lp = new LinearPotentiometer();
     lp.setPosition(0);
     lp.setOldPosition(0);
     // The first parameter is the value of the pin on
     // which the linear potentiometer is connected
     // lp will be converted to a string in the send method
     // Finally, the JSON will be like this:
     // {"14": {"oldPosition": 0, "position": 0}}
     super.send(TinkerShield.o_1, lp);
   }
}
```

### Utilisation de la simulation
L’utilisation est extrêment simple, deux classes sont à notre disposition HardwareSpeaker, en remplacement de l’Arduino et ConnectionSimulator, utilisé pour gérer la simulation des deux ports, c’est-à-dire socat.
Tout d’abord, il est nécessaire de lancer la simulation de ports séries. Pour ce faire :

```java
ConnectionSimulator simulator = new ConnectionSimulator();
```

Si pour une raison ou un autre, utiliser un Singleton est plus adapté :

```java
ConnectionSimulator.getInstance();
```

Le premier appel à la méthode getInstance démarrera socat.

HardwareSpeaker, la classe pour simuler une connexion par port série, possède deux méthodes. La première `speak(java.util.String)``, permet de simuler l’envoi de données sur le port série. Typiquement, elle permet de simuler un Arduino envoyant des données sur le port série. La deuxième méthode, `listen`, permet de lire les informations sur le port série. Dans ce cas, c’est l’inverse, cela peut par exemple simuler le fait qu’un Arduino lise des données qui ont été envoyées sur le port série.

Par exemple, pour envoyer votre premier message à la place de l’Arduino :
```java
simulator.getHardwareSpeaker().speak("Hello world!");
```

Ainsi l’application normalement connecté à l’Arduino et écoutant sur le port série approprié (pour la simulation le port série est indiqué dans la propriété système `xwot.test.port`), recevra Hello world!
A son tour, l’application envoie le message Hello people! et pour simuler le fait que l’Arduino lit l’information :

```java
simulator.getHardwareSpeaker().listen(); // return Hello people!
```

Comme vous le voyez, l’utilisation est simple et peut être adaptée à beaucoup de cas. Cependant, si la bibliothèque ArduinoComponents ou le principe de la représentation d’un composant Arduino par une classe Java est utilisée, le niveau d’abstraction devient plus élevé. En effet, la classe JaxbAbstractFactory permet véritablement de simuler l’envoi de JSON structuré, de la manière décrite à la section ci-dessus, par un Arduino. Cela s’effectue en quatre étapes :
￼
1. Initialiser la classe JaxbAbstractFactory
2. Initialiser une classe représentant un composant Arduino. Typiquement une de celle
contenue dans la bibliothèque ArduinoComponents
3. Assigner les valeurs souhaitées à l’objet créé au point deux
4. Utiliser la méthode send de JaxbAbstractFactory qui utilise la méthode speak de HardwareSpeaker afin de simuler l’envoi de JSON structuré à l’application

```java
AbstractJaxbFactory fac = new AbstractJaxbFactory(simulator.getHardwareSpeaker());
LinearPotentiometer pot = new LinearPotentiometer();
pot.setFromPercentPosition(100);
fac.send(TinkerShield.i_0, pot);
```

La ligne numéro trois assigne la position cent (en pour-cent), qui est ensuite transpo- sée sur une échelle de 0 à 1023. La ligne numéro quatre encode l’objet pot en JSON et envoie la chaîne obtenue sur le port série de l’application grâce à la méthode speak de HardwareSpeaker. TinkerShield.i_0 correspond au pin I0 de l’Arduino (valeur qua- torze) et donc le JSON produit sera de la forme :

```json
{"14": {"position": 1023, "oldPosition": 0}}
```

Où 14 est la valeur numérique de I0, côté serveur stockée dans l’enum TinkerShield et permet donc d’identifier le composant Arduino avec précision, puisque cela indique sur quel pin de l’Arduino le composant de type potentiomètre linéaire est connecté.

Pour utiliser la classe AbstractJaxbFacory, nous recommandons la création d’une Factory qui étend de cette classe. Par contre, ce que ne fait pas la classe AbstractJaxbFactory c’est de pouvoir envoyer plusieurs objets dans une même chaîne de caractères JSON. La lecture de messages JSON envoyés par l’application est possible grâce à la méthode listen de HardwareSpeaker, mais cette méthode renvoie uniquement une chaîne de caractères et ne permet ni de parser le JSON ni de le trans- former en instance de classe représentant un composant. Cependant, ceci pourrait être implémenté sans trop de mal en s’inspirant de la bibliothèque ArduinoCommunication.
