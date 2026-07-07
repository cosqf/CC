# Comunicações por Computador - Projeto 25/26

---
### Grade: 16/20 ⭐
---

###### PL5 Group 2: Cátia Eira (a107382), Filipa Cosquete (a106837), Fábio Silva (a82331)

---

A Java program that simulates the communication between a mothership, one or more simultaneous rovers performing missions on Mars, and a ground control entity.

---
## Recommended Software:
 - Oracle VirtualBox;
 - Virtual Machine with ubuntu and core;
 - Developed and tested on Java 21. Compatibility with earlier versions is not guaranteed;
 - Maven is recommended, but manual javac compilation instructions are provided;

## Setting up the Virtual Machine (VM)
You may either keep the project files in a shared folder accessible from your VM, or have the project folder directly inside the VM.  

#### Using a Shared Folder

1. Open VirtualBox → Settings → Shared Folders

2. Add the folder that contains the project

3. Enable Auto-mount

Inside the VM, grant your user access to shared folders by using:

`sudo usermod -aG vboxsf $USER`

Restart the VM for the change to take effect.

> Note: the native folder should be named CC so that the shared folder's path will automatically be ```/media/sf_CC/```

#### Keeping the project in the VM

Make sure the folder's path is ```/home/core/CC/```


#### Installing the recommended software

You can install Java 21 and Maven manually, or use the provided configuration script:
`./scripts/vmconfig.sh`

This script will automatically install the required packages.

## Available Scripts:

> Note: only `setup.sh` must be run as `./scripts/setup.sh`, `vmconfig.sh` may be used inside or outside scripts/ using the appropriate execution command.
---

`vmconfig.sh`  
Installs Java 21 and Maven inside the VM  
#### Usage:
```
sudo ./scripts/vmconfig.sh
```
---
`setup.sh`  
Compiles program files using maven or javac, and copies the remaining scripts to their proper directories in core.  
The first time compiling with Maven will take a bit longer than subsequent compilations.
#### Usage options:

``` bash
./scripts/setup.sh #to compile with javac
./scripts/setup.sh mvn #to compile with maven
```
---
`mothership.sh`  
Launches the mothership process.  
#### Usage options:
``` bash
./mothership.sh #if the repo is inside the VM
./mothership.sh sf #if the repo is a shared folder
```
---
`rover.sh`  
Launches a rover instance with ID = [ID], where [ID] is a natural number.
#### Usage options:
``` bash
./rover.sh [ID] #if the repo is inside the VM
./rover.sh [ID] sf #if the repo is a shared folder
```
---
`groundcontrol.sh`  
Launches the ground control process.
#### Usage options:
``` bash
./groundcontrol.sh #if the repo is inside the VM
./groundcontrol.sh sf #if the repo is a shared folder
```
---

## Running the System (Typical Workflow)
> These steps assume the VM is configured to your preference, and running.
1. Start core.
2. Load the provived **Topology.xml** or **Topology.imn** file.
3. Start the session.
4. Open a VM terminal (not on core!), navigate to your project folder, and run `./scripts/setup.sh`.
5. Open a terminal in the Mothership node, and run `./mothership.sh`.
6. Open a terminal in the Ground Control node, and run `./groundcontrol.sh`.
7. Open a terminal in a Rover node, and run `./rover.sh [ID]`.
8. Repeat step 7 for as many rovers as desired. 

> Note: if any rovers are added to the topology, `setup.sh` must be run again to ensure the new rovers have the `rover.sh` script available.
