#!/usr/bin/env python3

import sys
import numpy as np
import scipy.sparse as sp
import scipy.io as io

DATA=sys.argv[1]
CCLayersPATH=DATA+'/'+DATA+'.cc-layers'
SAVEPATH=DATA+'/ccdist/layer'
def getlayer(layer):
    return np.fromfile(''.join([DATA,'/edgedecomp/',DATA,'.layer',str(layer),'.nd']), dtype='>i4')

a = np.fromfile(CCLayersPATH, dtype='>i4').reshape(-1,3)

layers = set()
for x in range(0,len(a)):
    if a[x][0] < 0:
        layer = -a[x][0]
        # layers[layer] = getlayer(layer)
        layers.add(layer)
    else:
        a[x][0] = layer

# a = a[a[:,2].argsort(kind='stable')]

for L in layers:
    lind = np.where(a[:,0]==L)
    ll = a[lind]
    lld = {}
    # ll = ll[ll[:,2].argsort(kind='stable')]
    for x,y,z in ll:
        if y not in lld:
            lld[y] = lld.get(y,[0,z])
        else:
            lld[y][0]+=1

    # print(lld)
    lld = sorted(lld.items(),key=lambda x:x[1][0])
    lld = sorted(lld,key=lambda x:x[1][1])
    with open(SAVEPATH+str(L)+'.ccd', 'w') as f:
        for k,v in lld:
            f.write("%s\t%s\t%s\n" % (str(k),str(v[0]),str(v[1])))
