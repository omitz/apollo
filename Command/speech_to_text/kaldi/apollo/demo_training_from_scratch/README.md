# Train Kaldi Model from Annotated Dataset
## Instruction

### 1.) Download ans4 data 
```bash
cd ../../egs/ans4/s5/
sed -i 's|\"queue\.pl|\"run\.pl|g' cmd.sh
sed -i 's|=queue\.pl|=run\.pl|g' cmd.sh
sed -i 's|/home/allen/data|$(readlink -f ./)|g' run.sh
bash -x ./run.sh  ## it may take a while
cd -
```


### 1.) create a directory \<whatever> under kaldi/egs/
```bash
mkdir -p ../../egs/apollo
```

### 2.) Copy these scripts to \<whatever>
```bash
cp *.bash ../../egs/apollo/
```

### 3.) Run the scripts:
```bash
cd ../../egs/apollo/
./prepare_train.bash
./train.bash
./test.bash
```


