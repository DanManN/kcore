#!/usr/bin/env python3

# DATA="$1"
# RANGES=()
# CORES=()
# prev=-1
# while read line
# do
#         lrn=($line)
#         if (( lrn[0] < 0 )); then
#                 CORES+=($((-lrn[0])))
#                 RANGES[-lrn[0]]=$((lrn[1] + RANGES[1-lrn[0]]))
#         else
#                 for x in ${CORES[@]}; do
#                         if (( lrn[1] < RANGES[$x] )); then
#                                 DEG=$(($(./bindump.sh $DATA/edgedecomp/$DATA.layer$x.nd -N4 -j $((4*lrn[0])))))
#                                 if (( prev == lrn[0] )); then
#                                         echo -n " $x,$DEG"
#                                 else
#                                         echo -ne "\n${lrn[0]} $x,$DEG"
#                                 fi
#                                 prev=${lrn[0]}
#                                 break
#                         fi
#                 done
#         fi
# done < <(./bindump.sh $DATA/$DATA.cc-layers -w12 | sort -nsk 1)
import sys
import numpy as np
import scipy.sparse as sp
import scipy.io as io

DATA=sys.argv[1]
CCLayersPATH=DATA+'/'+DATA+'.cc-layers'
def getlayer(layer):
    return np.fromfile(''.join([DATA,'/edgedecomp/',DATA,'.layer',str(layer),'.nd']), dtype='>i4')

a = np.fromfile(CCLayersPATH, dtype='>i4').reshape(-1,3)

layers = {}
for x in range(0,len(a)):
    if a[x][0] < 0:
        layer = -a[x][0]
        layers[layer] = getlayer(layer)
    else:
        a[x][2] = layer

a = a[a[:,0].argsort(kind='stable')]
# ranges = dict(a[np.where(a<0)[0]][:,:2])
# cum = 0
# for l,r in ranges.items():
#     ranges[l]+=cum
#     cum+=r

# ldmat = {}
maxLayer = -a[0][0]
numverts = a[-1][0]
vldmat = sp.dok_matrix((numverts+1, maxLayer+1), dtype=np.int32)
for v,c,l in a[len(layers):]:
    # for l,r in ranges.items():
    #     if c < r:
    #         ldmat[v] = ldmat.get(v, [])+[(v,-l)]
    #         break
    # ldmat[v] = ldmat.get(v, [])+[(v,l)]
    vldmat[v,l] = layers[l][v]

print(vldmat)
io.savemat(DATA+'/'+DATA+'.vld',vldmat)
