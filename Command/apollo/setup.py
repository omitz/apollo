import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

with open('requirements.txt') as f:
    requirements = f.read().splitlines()

setuptools.setup(
    name="apollo",
    version="0.0.1",
    author="CACI",
    #author_email="author@example.com",
    description="Apollo Python package",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/NextCenturyCorporation/apollo",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    python_requires='>=3.6',
    install_requires=requirements
)