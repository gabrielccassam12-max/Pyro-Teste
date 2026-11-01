# Pyro: Despertar do Fogo (mod de teste)

Mod de teste com **efeitos de fogo 3D de verdade** (modelos animados GeckoLib, não partículas planas).

## Precisa instalar (pasta mods)
1. Fabric Loader + **Fabric API** (1.21.1)
2. **GeckoLib** para Fabric 1.21.1 (versão 4.7.x)

## Como despertar
- Craftar a **Brasa do Despertar** (4 blaze rods em cruz + 1 diamante no meio) e clicar com o botão direito, ou
- Levar um golpe que te deixe com pouca vida (15% de chance de o fogo despertar), ou
- Op: `/pyro awaken` (com animação), `/pyro instant`, `/pyro master`, `/pyro energy <n>`, `/pyro reset`

## Poderes (teclas Z X C V B, dá pra mudar em Controles)
| Tecla | Poder | Efeito 3D |
|---|---|---|
| Z | Bola de Fogo | esfera de fogo com chamas girando |
| X | Pilar de Chamas | coluna de fogo no ponto mirado (8 blocos) |
| C | Nova de Fogo | anel de chamas que se expande ao redor |
| V | Meteoro | meteoro caindo no ponto mirado, com cauda de fogo |
| B | Aura de Fogo | chamas orbitando seu corpo (liga/desliga) |

Quanto mais usa, mais sobe a maestria (Faísca → Chama → Inferno → Sol): mais calor máximo e mais dano.

Os explosivos acendem fogo no chão. Para desligar: `FireOps.SCORCH_BLOCKS = false` no código, ou desligue a regra `doFireTick`.

Aviso: código não testado dentro do jogo.
