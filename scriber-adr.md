# Project Scriber ADR

Project Scriber is an attempt to deliver a truly declarative data interface for government webforms, powered by the Fact Graph.
It includes an XML data specification for flow building and two generators that output that specification in different formats:
a multi-page form application and an all-screens data view.

Project Scriber builds on the experience and learnings of the DF Flow, and is inspired by its JSX interface.
Its first implementation will be for the Tax Witholding Estimator (TWE).

## Background: The DF Flow

The [DF Flow](https://github.com/IRS-Public/direct-file/blob/e0d5c84451cc52b72d20d04652e306bf4af1a43c/direct-file/df-client/df-client-app/src/flow/flow.tsx) was built in React and specified with JSX.
`Flow.tsx` contains the following statement of intent (slightly excerpted):

> The flow, while currently existing as code, has an eventual goal of becoming configuration that tax experts will be able to modify.
> This leads to a few design choices:
>   1. Everything in this file should remain serializable.
>   2. While many of the below react components look like our fact components, they have separate definitions from the components that actually render.
>   3. We have to maintain a mapping between the components we declare here in our config and the components that eventually render.

The Flow made tremendous progress in this direction, but it did not succeed.
Non-engineering tax experts could not modify the Flow, and the Flow was not serializable (more on the implications of that below).
Scriber attempts to carry forward the successes of the Flow, and improve on where it fell short

### Successes

The DF Flow models a linear question flow, while also auto-generating "Data Views" which let users go back and edit the information they input.

A couple of its core successes:

* Attributes like the `path` attribute, which links an input to a specific fact, and the `condition` attribute, which only shows a screen if the fact graph matches a logical statement, seamlessly connect the flow and Fact Graph
* It's (usually) easy to add a new modal or paragraph to a given screen
* All Screens unlocked *many* workflows for technical and non-technical stakeholders alike.

### Limitations

The main shortcoming of the Flow that Scriber seeks to resolve is that the Flow has no data representation.
The Flow is built in JSX, an [XML-like ECMAScript extension](https://facebook.github.io/jsx/).
JSX is is declarative, but it is not data.
This imposes major limitations on how the flow works.

To start, there aren't really tools that "read" JSX.
JavaScript build tools transpile JSX to `React.createElement` (or other framework equivalent).
Therefore any tool that wants to understand the Flow has to run React—and an entire browser.

For example, All Screens was (correctly) hailed as a success story for the Flow's declarative specification, but there's a reason the tooling never evolved beyond that.
All Screens doesn't introspect JSX, [it introspects a tree of React elements](https://github.com/IRS-Public/direct-file/blob/e0d5c84451cc52b72d20d04652e306bf4af1a43c/direct-file/df-client/df-client-app/src/all-screens/AllScreensContent.tsx#L98).
By the time the flow has been converted into React elements, there's not much you can do with them *besides* render them in unique configurations;
Most of the "declarative" information has been lost.

More importantly, JSX is essentially impossible to edit programatically.
The tooling does not exist to pass a program some JSX and then have it output new JSX for you.
JSX is source code, so flow updates require source code updates, limiting the pool of potential contributors to engineers.
In theory, it would be possible to teach non-technical stakeholders to make source code updates to a structured data format, but the difficulty of getting such stakeholders to be comfortable with the slew of build tools required to validate and view JSX made that a practical impossibility.

This was the key limitation that prevented the creation of a "Taxpert" interface for DF.
A taxpert program needs to read the current flow data, display it in an intuitive interface, and then output a new flow *as data*.
For that to happen, the flow has to be data.

Fortunately, a JSX-like data format exists that resolves all these problems.
It can be read and written by lightweight tooling that exists for every programming language;
it can be introspected as a graph without loss of any information;
and it can trivially rendered in a browser.
That format is XML (or XHTML).

## The XML Flow
