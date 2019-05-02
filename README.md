# Android Worker

The Android Worker is an app that connects to a PySyft worker and performs the operations in its behalf. It is part of a setup that uses a socket server, a socket client and the app itself which is another socket client

## Quick start

* Start the socket server
  * There is an example provided in the code: `socketio_server_demo.py`
* Start a Jupyter notebook and create a `WebsocketIOClientWorker` object.
  * Note that you need to provide strategies for serialization and compression
  * See the example `examples/experimental/Sockets/Socket%20Bob.ipynb`
* Run the app
* Execute the operations in the notebook and see how Android handles them!

#### Notes
* The project is still in an early stage and only some PySyft operations are implemented: `send`, `get`, `add`, `delete`
* Make sure the socket server, the client in the notebook and the app all point to the same host/port
* To run the setup locally, it is better to use an Android emulator


