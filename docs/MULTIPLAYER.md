# Partida compartilhada — Minecraft 1.21 / Fabric

Cada bloco de mesa mantém uma única partida no servidor. Todos os jogadores que
abrem **o mesmo bloco** recebem as mesmas bolas e a mesma vez de jogo. Mesas
diferentes mantêm partidas independentes.

## Como jogar

1. Instale a mesma versão do mod e a Fabric API no servidor e nos clientes.
2. No mesmo mundo ou servidor, aproxime os jogadores da mesma mesa.
3. Clique com o **botão direito** na mesa para abrir a tela.
4. A ordem de abertura define a ordem das jogadas. A tela mostra **Sua vez** ou
   **Vez de …**, além do número de jogadores e um placar ao lado da mesa.
5. Coloque o **seu taco de sinuca no slot da tela**, ao lado da barra de força. O
   slot aceita somente um taco utilizável. Cada jogador vê apenas o próprio taco
   nesse mesmo slot; os tacos e suas durabilidades são individuais.
   Clicar no bloco com o taco na mão abre a tela; a inserção é feita pelo slot
   dentro dela.
6. Na sua vez, clique e arraste a partir da bola branca para escolher a direção.
   Solte para confirmar a mira; depois clique/arraste a barra de força e solte
   para dar a tacada.
7. Todos acompanham a tacada. Cada bola colorida encaçapada vale **1 ponto** para
   quem deu a tacada. Quando as bolas param, quem pontuou continua jogando. Uma
   tacada sem encaçapar passa a vez ao próximo.

O taco básico tem **25 de durabilidade**; outros tipos podem ter valores próprios.
Cada tacada aceita consome uma unidade do taco de quem jogou. Mirar, colocar o
taco no slot ou tentar jogar fora da vez não consome durabilidade. A última
tacada é executada e o taco quebra, liberando o slot daquele jogador. Ele precisa
colocar outro para continuar, mantendo as bolas, os pontos e a ordem.

A tela mostra os usos restantes e a durabilidade máxima do taco colocado, por
exemplo `24/25` ou `59/60`. Sem taco, mostra `--`. O taco de outro jogador não
habilita suas tacadas e não sofre desgaste quando você joga.

Ao fechar a mesa, o taco retorna ao inventário do dono, mantendo o desgaste.
Se o inventário estiver cheio ou o jogador tiver morrido, o item é solto pelo
mecanismo normal do Minecraft. A durabilidade acompanha o item ao retirar,
guardar ou recolocar o taco, inclusive depois que a partida reiniciar.

Se a branca não acertar nenhuma bola, o jogador também **perde a vez por falta**.
Tocar apenas a borda não conta como acerto; bater na borda e depois em uma bola
conta normalmente. O placar mostra **Falta: nenhuma bola** quando a tacada termina.

Com apenas um jogador, a mesa permite treino. Com dois ou mais, todos alternam
na ordem de entrada. Fechar a tela retira o jogador da ordem e libera a vez.
Enquanto outro participante continuar com a mesa aberta, a partida continua com
as mesmas bolas e os mesmos pontos. Quem voltar entra no final da ordem.

Quando o **último participante fecha ou sai**, a partida reinicia: o placar é
zerado, as 16 bolas voltam à formação inicial e qualquer tacada em andamento é
cancelada. Com dois jogadores, ambos precisam sair para reiniciar; com três ou
mais, todos precisam sair. No treino solo, fechar a mesa já reinicia o jogo.
Sair do servidor, morrer ou se afastar da mesa também remove o participante.
Jogadores no modo espectador apenas acompanham.

Se a bola branca cair, ocorre uma **falta**: ela não vale ponto e o jogador perde
a vez, mesmo que também tenha encaçapado uma colorida. Os pontos das coloridas
continuam contando. A branca volta a uma posição livre quando as bolas param;
até lá, ninguém pode dar outra tacada. O placar indica quem cometeu a falta.

As bolas e os pontos ficam salvos junto com a mesa enquanto a partida continua.
Uma tacada em andamento mantém a identificação de quem a deu e o registro de
contato com outra bola, para atribuir corretamente os pontos e as faltas de um
estado salvo. Sair e voltar preserva os pontos quando outro participante ficou
com a mesa aberta. Se houver muitos nomes no placar, use a roda do mouse sobre
ele para consultar os demais.

A bola 8 vale um ponto nesta modalidade de contagem. Grupos de bolas e vitória
pelas regras de bola 8 ainda precisam de um modo de jogo próprio.

## Organização do projeto

| Parte | Responsabilidade |
| --- | --- |
| `BilliardsMod` / `BilliardsModClient` | Registram o conteúdo, a tela e as mensagens de rede de cada lado. |
| `block/`, `item/` e `assets/` | Bloco da mesa, itens, grupo criativo, modelos, texturas e traduções. O modelo no mundo é estático; a partida é exibida na tela. |
| `PoolCueItem` / `PoolCueSlot` / `PlayerCueInventory` | Durabilidade por tipo de taco, consumo por tacada válida e um slot individual por jogador, na mesma posição da tela. |
| `BilliardsBlockEntity` | Mantém o jogo de uma mesa, salva/carrega bolas, placar e tacadas pendentes e remove participantes que deixaram de usá-la. |
| `BilliardsGame` / `PlayerScore` / `FoulReason` | Controlam participantes, pontuação, continuidade da vez, faltas, reposição e reinício após a última saída. O jogo executa apenas no servidor. |
| `Ball`, `BallRack`, `BilliardsPhysics`, `TableBounds` | Dados das bolas, formação inicial, movimento, atrito, colisões e caçapas. |
| `BilliardsScreenHandler` | Compartilha a partida e mantém o taco individual de cada jogador. Ao fechar, devolve o item ao dono. Ao abrir, envia a partida completa. |
| `BilliardsScreen` / `BallRenderer` | Desenham a partida e o placar recebidos, suavizam as posições entre atualizações e coletam direção e força; não simulam uma partida particular. |
| `ShootPacket` | Solicita uma tacada, identificando a tela, a mesa e a vez. O servidor valida antes de aplicar. |
| `BilliardsState` / `TableStatePacket` | Transportam cópias das bolas, participante atual, placar, faltas, quantidade de jogadores e indicação de movimento. |
| `BilliardsModDataGenerator` / `ExampleMixin` | Estruturas iniciais do projeto, atualmente sem lógica de jogo. |

A posição das bolas usa coordenadas da mesa, independentes do tamanho da janela
e da escala da interface. Enquanto há movimento, os jogadores recebem estados
a cada tick do servidor. A física faz três etapas pequenas por tick (60 etapas
por segundo), e a tela interpola os estados para suavizar o movimento. O limiar
de parada evita um arrasto longo no fim da tacada. Uma mesa parada não gera
atualizações contínuas do jogo.
Mensagens de outra tela/mesa ou de uma vez anterior não podem aplicar tacadas.

## Novos tipos de taco

Defina a durabilidade nas configurações de cada item ao registrá-lo em
`ModItems`, usando `PoolCueItem`. Por exemplo:

```java
registerItem("novo_taco", new PoolCueItem(new Item.Settings().maxDamage(60)));
```

O slot, o desgaste, a quebra e o contador usam a durabilidade do item colocado.
A classe base não impõe um número fixo de tacadas.

Ao abrir uma mesa de uma versão com slot compartilhado, o item que estava nesse
slot antigo é devolvido uma única vez ao inventário do primeiro participante
que abrir a mesa.

## Verificação

Execute `./gradlew build` (Windows: `./gradlew.bat build`). Os testes cobrem
alternância com dois e três participantes, entrada durante uma tacada, saída e
retorno, tacadas concorrentes/fora da vez, valores inválidos, reposição da branca,
isolamento entre mesas e envio/leitura do mesmo estado para dois clientes. Também
cobrem pontos por bola, continuidade da vez, falta com e sem colorida, pontos de
quem saiu durante uma tacada, retomada após salvar, prevenção de pontos em dobro
e velocidade de movimento sem perder colisões ou caçapas. Também verificam a
falta por não acertar nenhuma bola, contato após uma batida na borda, manutenção
do jogo com um participante restante e reinício após todos saírem, inclusive
durante uma tacada.
Os testes do taco usam Fabric Loader JUnit para validar os itens reais do
Minecraft: exigência do taco, desgaste apenas em tacadas aceitas, quebra no
limite de cada item, troca por outro sem reiniciar, conservação da durabilidade
e restrições do slot. Incluem tacos de 3, 25 e 60 usos e dois jogadores com
slots individuais, impedindo o uso e o desgaste do taco alheio.

Para validar visualmente com duas instâncias do Minecraft, confira:

- As duas telas mostram as mesmas posições antes e durante a tacada.
- Apenas quem tem a vez consegue mirar e tacar.
- Cada jogador vê seu próprio taco no único slot da tela e o contador correspondente.
- Cada tacada consome um uso apenas do taco de quem jogou.
- O taco quebra ao atingir sua própria durabilidade máxima e exige a colocação de outro.
- Fechar a mesa devolve o taco ao dono, preservando o desgaste.
- Colocar outro taco permite continuar a partida com as mesmas bolas e pontos.
- O placar é o mesmo nas duas telas e aumenta uma vez por colorida encaçapada.
- Quem pontua com um acerto válido mantém a vez; quem erra ou encaçapa a branca passa a vez.
- Uma tacada sem tocar outra bola mostra a falta e passa a vez.
- O próximo jogador aguarda todas as bolas pararem, inclusive após uma falta.
- Abrir a mesa durante uma tacada mostra o movimento atual, sem remontar as bolas.
- Fechar a tela de quem joga libera a vez sem pular o próximo participante.
- Sair e voltar preserva bolas e pontos se outro participante continuar com a mesa aberta.
- Quando o último participante fecha a mesa, o placar zera e as bolas são remontadas.
