from distutils.core import setup


reqs = [
      'SQLAlchemy==1.3.18', 
      'sqlalchemy-utils==0.36.8', 
      'boto3==1.10.47', 
      'mpu==0.20.0', 
      'pika==1.1.0', 
      'urllib3==1.24.3',
      'psycopg2-binary==2.8.5',
      'py2neo==4.3.0', 
      'numpy==1.18.2', 
      'werkzeug==1.0.1',
      'watchdog[watchmedo]'
]

setup(name='commandutils',
      version='0.1',
      packages=['commandutils'],
      install_requires=reqs)
